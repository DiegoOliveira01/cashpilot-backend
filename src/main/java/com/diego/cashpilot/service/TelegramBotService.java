package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CategorySummaryDTO;
import com.diego.cashpilot.dto.TelegramUpdate;
import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.dto.TransactionResponseDTO;
import com.diego.cashpilot.dto.TransactionSummaryDTO;
import com.diego.cashpilot.model.Category;
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
    private static final int TOP_CATEGORIES = 3;
    private static final String NO_ICON = "🏷️";

    private static final String HELP = """
            Você pode escrever livremente:
            gastei 40 no mercado
            recebi 1.500 de salário ontem
            paguei 120 de luz dia 15

            O bot descobre a categoria pela descrição. Para escolher, use #:
            /gasto 35,90 jantar #lazer

            Comandos:
            /gasto 35,90 mercado
            /receita 1500 salário
            /saldo — resumo do mês atual
            /ultimos — últimos 5 lançamentos
            /desfazer — remove o último lançamento
            /categorias — lista suas categorias
            /categoria nome — muda a categoria do último lançamento

            Sem data, o lançamento vai para hoje.
            """;

    private static final String HELP_AI =
            "\nFrases mais livres? Veja /ia (interpretação por IA, opcional).";

    private static final String AI_NOTICE = """
            🤖 Interpretação por IA (opcional)

            Quando o bot não entende sua frase, ela pode ser enviada ao Gemini (Google, camada gratuita) para ser interpretada. Só o texto daquela mensagem e os nomes das suas categorias são enviados, sem seu nome, e-mail ou histórico.

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
    private final CategoryService categoryService;
    private final GeminiClient gemini;
    private final AiUsageLimiter aiLimiter;

    // Categoria escolhida para um lançamento e, se a #tag não existir, um aviso para o usuário.
    private record CategoryChoice(Category category, String warning) {}

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
            case "/categorias" -> withUser(chatId, user -> client.sendMessage(chatId, categoriesMessage(user)));
            case "/categoria" -> withUser(chatId, user -> handleSetCategory(chatId, user, text));
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

    private void handleCommandTransaction(long chatId, User user, String rawText) {
        HashtagExtractor.Result tagged = HashtagExtractor.extract(rawText);

        try {
            TelegramCommandParser.ParsedTransaction parsed = parser.parse(tagged.text());
            List<Category> available = categoryService.findAvailableForUser(user);
            CategoryChoice choice = chooseCategory(available, parsed.type(), tagged.tag(), null,
                    parsed.description());

            register(chatId, user, parsed.type(), parsed.amount(), parsed.description(),
                    LocalDate.now(ZONE), choice, null);
        } catch (IllegalArgumentException e) {
            client.sendMessage(chatId, "⚠️ " + e.getMessage());
        }
    }

    private void handleFreeText(long chatId, User user, String rawText) {
        HashtagExtractor.Result tagged = HashtagExtractor.extract(rawText);
        String text = tagged.text();
        LocalDate today = LocalDate.now(ZONE);
        List<Category> available = categoryService.findAvailableForUser(user);

        // 1ª camada: regras. Grátis, local e previsível.
        Optional<TelegramFreeTextParser.Result> byRules = freeTextParser.parse(text, today);
        if (byRules.isPresent()) {
            TelegramFreeTextParser.Result r = byRules.get();
            CategoryChoice choice = chooseCategory(available, r.type(), tagged.tag(), null, r.description());
            register(chatId, user, r.type(), r.amount(), r.description(), r.date(), choice,
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

        Optional<TelegramFreeTextParser.Result> byAi = gemini
                .extract(text, today,
                        namesOf(available, TransactionType.EXPENSE),
                        namesOf(available, TransactionType.INCOME))
                .flatMap(extraction -> extraction.toResult(today));
        if (byAi.isEmpty()) {
            client.sendMessage(chatId, NOT_UNDERSTOOD);
            return;
        }

        TelegramFreeTextParser.Result r = byAi.get();
        CategoryChoice choice = chooseCategory(available, r.type(), tagged.tag(), r.categoryHint(),
                r.description());
        register(chatId, user, r.type(), r.amount(), r.description(), r.date(), choice,
                "🤖 Interpretado por IA. Confira e use /desfazer se estiver errado.");
    }

    /**
     * Ordem de prioridade: #tag do usuário → sugestão da IA → palavras-chave da descrição.
     * Se a #tag não existir, o lançamento segue com a detecção automática e o usuário é avisado.
     */
    private CategoryChoice chooseCategory(List<Category> available, TransactionType type, String tag,
                                          String aiHint, String description) {
        String warning = null;

        if (tag != null) {
            Optional<Category> byTag = CategoryMatcher.findByName(tag, type, available);
            if (byTag.isPresent()) return new CategoryChoice(byTag.get(), null);
            warning = "⚠️ Categoria #" + tag + " não encontrada (veja /categorias).";
        }

        if (aiHint != null) {
            Optional<Category> byAi = CategoryMatcher.findByName(aiHint, type, available);
            if (byAi.isPresent()) return new CategoryChoice(byAi.get(), warning);
        }

        return new CategoryChoice(CategoryMatcher.match(description, type, available).orElse(null), warning);
    }

    private void register(long chatId, User user, TransactionType type, BigDecimal amount,
                          String description, LocalDate date, CategoryChoice choice, String footer) {
        Category category = choice.category();

        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setDescription(description);
        dto.setAmount(amount);
        dto.setType(type);
        dto.setDate(date);
        dto.setCategoryId(category == null ? null : category.getId());

        transactionService.createForUser(user, dto);

        StringBuilder reply = new StringBuilder("✅ ")
                .append(typeLabel(type)).append(" registrada: ").append(money(amount))
                .append(" — ").append(description)
                .append(" (").append(date.format(DAY_MONTH)).append(")");

        if (category != null) {
            reply.append("\n").append(iconOf(category)).append(" ").append(category.getName());
        } else {
            reply.append("\nSem categoria. Defina com /categoria nome.");
        }
        if (choice.warning() != null) reply.append("\n").append(choice.warning());
        if (footer != null) reply.append("\n").append(footer);

        client.sendMessage(chatId, reply.toString());
    }

    private String categoriesMessage(User user) {
        List<Category> available = categoryService.findAvailableForUser(user);

        return "📂 Suas categorias\n\n"
                + "Despesas:\n" + listOf(available, TransactionType.EXPENSE) + "\n\n"
                + "Receitas:\n" + listOf(available, TransactionType.INCOME) + "\n\n"
                + "Use #nome no lançamento (/gasto 40 jantar #lazer) ou /categoria nome para mudar a do último.\n"
                + "Para criar ou editar categorias, use o dashboard.";
    }

    private String listOf(List<Category> categories, TransactionType type) {
        List<String> items = categories.stream()
                .filter(c -> c.getType() == type)
                .map(c -> iconOf(c) + " " + c.getName())
                .toList();
        return items.isEmpty() ? "(nenhuma)" : String.join("\n", items);
    }

    private void handleSetCategory(long chatId, User user, String text) {
        String arg = text.contains(" ") ? text.substring(text.indexOf(' ') + 1).trim() : "";
        if (arg.isEmpty()) {
            client.sendMessage(chatId,
                    "Uso: /categoria nome (muda a categoria do último lançamento)\n\n" + categoriesMessage(user));
            return;
        }
        if (arg.startsWith("#")) arg = arg.substring(1);

        Optional<TransactionResponseDTO> last = transactionService.findLastCreatedForUser(user);
        if (last.isEmpty()) {
            client.sendMessage(chatId, "Não há lançamentos para categorizar.");
            return;
        }

        TransactionResponseDTO target = last.get();
        Category category = null;
        boolean remove = arg.equalsIgnoreCase("nenhuma") || arg.equalsIgnoreCase("sem categoria");

        if (!remove) {
            Optional<Category> found = CategoryMatcher.findByName(arg, target.getType(),
                    categoryService.findAvailableForUser(user));
            if (found.isEmpty()) {
                client.sendMessage(chatId, "Não achei essa categoria entre as de "
                        + typeLabel(target.getType()).toLowerCase() + ". Veja /categorias.");
                return;
            }
            category = found.get();
        }

        try {
            transactionService.recategorizeLatestForUser(user, category);
        } catch (IllegalArgumentException e) {
            client.sendMessage(chatId, "⚠️ " + e.getMessage());
            return;
        }

        String summary = target.getDescription() + " (" + money(target.getAmount()) + ")";
        client.sendMessage(chatId, category == null
                ? "🏷️ Categoria removida de: " + summary
                : "🏷️ " + summary + " agora é " + iconOf(category) + " " + category.getName() + ".");
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

    private void handleBalance(long chatId, User user) {
        YearMonth month = YearMonth.now(ZONE);
        TransactionSummaryDTO summary = transactionService.getSummaryForUser(user, month);

        StringBuilder reply = new StringBuilder("💰 Resumo de ").append(month.format(MONTH_FORMAT)).append("\n\n")
                .append("Receitas: ").append(money(summary.getIncome())).append("\n")
                .append("Despesas: ").append(money(summary.getExpense())).append("\n")
                .append("Saldo: ").append(money(summary.getBalance()));

        List<CategorySummaryDTO> top = transactionService.getSummaryByCategoryForUser(user, month).stream()
                .filter(c -> c.getType() == TransactionType.EXPENSE)
                .limit(TOP_CATEGORIES)
                .toList();

        if (!top.isEmpty()) {
            reply.append("\n\nMaiores gastos:");
            for (CategorySummaryDTO c : top) {
                reply.append("\n").append(c.getIcon() == null ? NO_ICON : c.getIcon())
                        .append(" ").append(c.getName()).append(" — ").append(money(c.getTotal()));
            }
        }

        client.sendMessage(chatId, reply.toString());
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
                    .append(income ? "+" : "-").append(money(t.getAmount()));
            if (t.getCategory() != null) {
                sb.append(" · ").append(t.getCategory().getIcon() == null ? NO_ICON : t.getCategory().getIcon())
                        .append(" ").append(t.getCategory().getName());
            }
            sb.append("\n");
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

    private List<String> namesOf(List<Category> categories, TransactionType type) {
        return categories.stream().filter(c -> c.getType() == type).map(Category::getName).toList();
    }

    private String iconOf(Category category) {
        return category.getIcon() == null ? NO_ICON : category.getIcon();
    }

    private String typeLabel(TransactionType type) {
        return type == TransactionType.INCOME ? "Receita" : "Despesa";
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
