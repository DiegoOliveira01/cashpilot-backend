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
            Comandos:
            /gasto 35,90 mercado
            /receita 1500 salário
            /saldo — resumo do mês atual
            /ultimos — últimos 5 lançamentos
            /desfazer — remove o último lançamento

            A descrição é opcional. O lançamento é registrado com a data de hoje.
            """;

    private static final String NOT_LINKED =
            "Sua conta ainda não está conectada. Use o botão \"Conectar Telegram\" no dashboard.";

    private final TelegramClient client;
    private final TelegramLinkService linkService;
    private final TelegramCommandParser parser;
    private final TransactionService transactionService;

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
        String command = text.split("\\s+")[0].toLowerCase().replaceAll("@.*$", "");

        switch (command) {
            case "/start" -> handleStart(chatId, text);
            case "/ajuda", "/help" -> client.sendMessage(chatId, HELP);
            case "/gasto", "/despesa", "/receita" ->
                    withUser(chatId, user -> handleTransaction(chatId, user, text));
            case "/saldo" -> withUser(chatId, user -> handleBalance(chatId, user));
            case "/ultimos" -> withUser(chatId, user -> handleLatest(chatId, user));
            case "/desfazer" -> withUser(chatId, user -> handleUndo(chatId, user));
            default -> client.sendMessage(chatId, "Comando não reconhecido. Use /ajuda.");
        }
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
                    "✅ Conta conectada, " + user.get().getName() + "!\n\n" + HELP);
        } else {
            client.sendMessage(chatId,
                    "Código inválido ou expirado. Gere um novo no dashboard.");
        }
    }

    private void handleTransaction(long chatId, User user, String text) {
        try {
            TelegramCommandParser.ParsedTransaction parsed = parser.parse(text);

            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setDescription(parsed.description());
            dto.setAmount(parsed.amount());
            dto.setType(parsed.type());
            dto.setDate(LocalDate.now(ZONE));

            transactionService.createForUser(user, dto);

            client.sendMessage(chatId,
                    "✅ " + typeLabel(parsed.type()) + " registrada: "
                            + money(parsed.amount()) + " — " + parsed.description());
        } catch (IllegalArgumentException e) {
            client.sendMessage(chatId, "⚠️ " + e.getMessage());
        }
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
