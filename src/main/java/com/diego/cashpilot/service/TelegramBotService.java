package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.TelegramUpdate;
import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.dto.TransactionResponseDTO;
import com.diego.cashpilot.dto.TransactionSummaryDTO;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class TelegramBotService {

    // O Railway roda em UTC: sem fixar o fuso, lançamentos à noite caem no dia seguinte.
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final int LATEST_LIMIT = 5;

    private static final String HELP = """
            Você pode escrever livremente:
            gastei 40 no mercado
            recebi 1.500 de salário ontem
            paguei 120 de luz dia 15

            Ou usar comandos:
            /gasto 35,90 mercado
            /receita 1500 salário
            /saldo — resumo do mês atual
            /ultimos — últimos 5 lançamentos
            /desfazer — remove o último lançamento

            Sem data, o lançamento vai para hoje.
            """;

    private static final String HELP_AI =
            "\nFrases mais livres? Veja /ia (interpretação por IA, opcional).";

    private static final String AI_NOTICE = """
            🤖 Interpretação por IA (opcional)

            Quando o bot não entende sua frase, ela pode ser enviada ao Gemini (Google, camada gratuita) para ser interpretada. Só o texto daquela mensagem é enviado, sem seu nome, e-mail ou histórico.

            ⚠️ Na camada gratuita, o Google pode usar esse texto para melhorar seus produtos, e ele pode ser revisado por pessoas. Este é um projeto de portfólio: não digite dados sensíveis.

            Para ativar: /ia on
            Para desativar: /ia off
            """;

    private static final String NOT_LINKED =
            "Sua conta ainda não está conectada. Use o botão \"Conectar Telegram\" no dashboard.";

    private static final String NOT_UNDERSTOOD =
            "Não consegui entender. Tente algo como \"gastei 40 no mercado\" ou use /gasto 40 mercado. "
                    + "Veja /ajuda.";

    private final TelegramClient client;
    private final TelegramLinkService linkService;
    private final TelegramCommandParser parser;
    private final TelegramFreeTextParser freeTextParser;
    private final TransactionService transactionService;
    private final GeminiClient gemini;
    private final AiUsageLimiter aiLimiter;

    // O Telegram reenvia o mesmo update se a resposta demorar (ex.: serviço acordando).
    // Isso também impede que um /desfazer reenviado apague dois lançamentos.
    private final AtomicLong lastUpdateId = new AtomicLong(0);

    public void handle(TelegramUpdate update) {
        TelegramUpdate.Message message = update.message();
        if (message == null || message.text() == null || message.chat() == null) return;
        if (!"private".equals(message.chat().type())) return; // ignora grupos e canais

        long previous = lastUpdateId.getAndAccumulate(update.updateId(), Math::max);
        if (update.updateId() <= previous) return; // duplicado

        long chatId = message.chat().id();
        String text = message.text().trim();

        // Texto livre: qualquer mensagem que não seja comando.
        if (!text.startsWith("/")) {
            withUser(chatId, user -> handleFreeText(chatId, user, text));
            return;
        }

        String command = text.split("\\s+")[0].toLowerCase().replaceAll("@.*$", "");

        switch (command) {
            case "/start" -> handleStart(chatId, text);
            case "/ajuda", "/help" -> client.sendMessage(chatId, help());
            case "/gasto", "/despesa", "/receita" ->
                    withUser(chatId, user -> handleCommandTransaction(chatId, user, text));
            case "/saldo" -> withUser(chatId, user -> handleBalance(chatId, user));
            case "/ultimos" -> withUser(chatId, user -> handleLatest(chatId, user));
            case "/desfazer" -> withUser(chatId, user -> handleUndo(chatId, user));
            case "/ia" -> withUser(chatId, user -> handleAi(chatId, text));
            default -> client.sendMessage(chatId, "Comando não reconhecido. Use /ajuda.");
        }
    }

    private String help() {
        return gemini.isAvailable() ? HELP + HELP_AI : HELP;
    }

    /** Executa a ação só se o chat estiver vinculado a um usuário. */
    private void withUser(long chatId, Consumer<User> action) {
        Optional<User> user = linkService.findUserByChatId(chatId);
        if (user.isEmpty()) {
            client.sendMessage(chatId, NOT_LINKED);
            return;
        }
        action.accept(user.get());
    }

    private void handleStart(long chatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            client.sendMessage(chatId,
                    "Olá! Para conectar sua conta, use o botão \"Conectar Telegram\" no dashboard do Cash Pilot.");
            return;
        }

        Optional<User> user = linkService.consumeCode(parts[1], chatId);
        if (user.isPresent()) {
            client.sendMessage(chatId,
                    "✅ Conta conectada, " + user.get().getName() + "!\n\n" + help());
        } else {
            client.sendMessage(chatId,
                    "Código inválido ou expirado. Gere um novo no dashboard.");
        }
    }

    private void handleCommandTransaction(long chatId, User user, String text) {
        try {
            TelegramCommandParser.ParsedTransaction parsed = parser.parse(text);
            register(chatId, user, parsed.type(), parsed.amount(), parsed.description(),
                    LocalDate.now(ZONE), null);
        } catch (IllegalArgumentException e) {
            client.sendMessage(chatId, "⚠️ " + e.getMessage());
        }
    }

    private void handleFreeText(long chatId, User user, String text) {
        LocalDate today = LocalDate.now(ZONE);

        // 1ª camada: regras. Grátis, local e previsível.
        Optional<TelegramFreeTextParser.Result> byRules = freeTextParser.parse(text, today);
        if (byRules.isPresent()) {
            TelegramFreeTextParser.Result r = byRules.get();
            register(chatId, user, r.type(), r.amount(), r.description(), r.date(),
                    "Errou? Use /desfazer.");
            return;
        }

        // 2ª camada: IA, só se estiver habilitada no ambiente E o usuário tiver aceitado (/ia on).
        if (!gemini.isAvailable()) {
            client.sendMessage(chatId, NOT_UNDERSTOOD);
            return;
        }
        if (!linkService.hasAiConsent(chatId)) {
            client.sendMessage(chatId, NOT_UNDERSTOOD
                    + "\n\nQuer que uma IA tente interpretar frases mais livres? Veja /ia.");
            return;
        }

        switch (aiLimiter.tryAcquire(user.getId())) {
            case USER_LIMIT -> {
                client.sendMessage(chatId,
                        "Você atingiu o limite diário de interpretações por IA. Use /gasto ou frases simples.");
                return;
            }
            case GLOBAL_LIMIT -> {
                client.sendMessage(chatId,
                        "O limite diário de IA do projeto foi atingido. Use /gasto ou frases simples.");
                return;
            }
            case ALLOWED -> { }
        }

        Optional<TelegramFreeTextParser.Result> byAi = gemini.extract(text, today)
                .flatMap(extraction -> extraction.toResult(today));
        if (byAi.isEmpty()) {
            client.sendMessage(chatId, NOT_UNDERSTOOD);
            return;
        }

        TelegramFreeTextParser.Result r = byAi.get();
        register(chatId, user, r.type(), r.amount(), r.description(), r.date(),
                "🤖 Interpretado por IA. Confira e use /desfazer se estiver errado.");
    }

    private void handleAi(long chatId, String text) {
        if (!gemini.isAvailable()) {
            client.sendMessage(chatId, "A interpretação por IA não está habilitada neste ambiente.");
            return;
        }

        String[] parts = text.split("\\s+");
        String arg = parts.length > 1 ? parts[1].toLowerCase() : "";

        switch (arg) {
            case "on" -> {
                linkService.setAiConsent(chatId, true);
                client.sendMessage(chatId,
                        "✅ IA ativada. Frases que o bot não entender serão interpretadas pelo Gemini. "
                                + "Desative quando quiser com /ia off.");
            }
            case "off" -> {
                linkService.setAiConsent(chatId, false);
                client.sendMessage(chatId,
                        "IA desativada. Seus textos não serão mais enviados ao Gemini.");
            }
            default -> {
                String status = linkService.hasAiConsent(chatId) ? "ativada ✅" : "desativada";
                client.sendMessage(chatId, AI_NOTICE + "\nStatus atual: " + status);
            }
        }
    }

    private void register(long chatId, User user, TransactionType type, BigDecimal amount,
                          String description, LocalDate date, String footer) {
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setDescription(description);
        dto.setAmount(amount);
        dto.setType(type);
        dto.setDate(date);

        transactionService.createForUser(user, dto);

        String reply = "✅ " + typeLabel(type) + " registrada: " + money(amount)
                + " — " + description + " (" + date.format(DAY_MONTH) + ")";
        if (footer != null) {
            reply += "\n" + footer;
        }
        client.sendMessage(chatId, reply);
    }

    private void handleBalance(long chatId, User user) {
        YearMonth month = YearMonth.now(ZONE);
        TransactionSummaryDTO summary = transactionService.getSummaryForUser(user, month);

        client.sendMessage(chatId,
                "💰 Resumo de " + month.format(MONTH_FORMAT) + "\n\n"
                        + "Receitas: " + money(summary.getIncome()) + "\n"
                        + "Despesas: " + money(summary.getExpense()) + "\n"
                        + "Saldo: " + money(summary.getBalance()));
    }

    private void handleLatest(long chatId, User user) {
        List<TransactionResponseDTO> latest = transactionService.findLatestForUser(user, LATEST_LIMIT);
        if (latest.isEmpty()) {
            client.sendMessage(chatId, "Você ainda não tem lançamentos.");
            return;
        }

        StringBuilder sb = new StringBuilder("🧾 Últimos lançamentos\n\n");
        for (TransactionResponseDTO t : latest) {
            boolean income = t.getType() == TransactionType.INCOME;
            sb.append(income ? "🟢 " : "🔴 ")
                    .append(t.getDate().format(DAY_MONTH)).append(" · ")
                    .append(t.getDescription()).append(" · ")
                    .append(income ? "+" : "-").append(money(t.getAmount()))
                    .append("\n");
        }
        client.sendMessage(chatId, sb.toString().trim());
    }

    private void handleUndo(long chatId, User user) {
        Optional<TransactionResponseDTO> removed = transactionService.deleteLatestForUser(user);
        if (removed.isEmpty()) {
            client.sendMessage(chatId, "Não há lançamentos para desfazer.");
            return;
        }

        TransactionResponseDTO t = removed.get();
        client.sendMessage(chatId,
                "🗑️ Removido: " + typeLabel(t.getType()) + " de " + money(t.getAmount())
                        + " — " + t.getDescription() + " (" + t.getDate().format(DAY_MONTH) + ")");
    }

    private String typeLabel(TransactionType type) {
        return type == TransactionType.INCOME ? "Receita" : "Despesa";
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
