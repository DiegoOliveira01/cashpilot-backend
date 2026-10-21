package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.TelegramUpdate;
import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TelegramBotService {

    // O Railway roda em UTC: sem fixar o fuso, lançamentos à noite caem no dia seguinte.
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static final String HELP = """
            Comandos:
            /gasto 35,90 mercado
            /receita 1500 salário

            A descrição é opcional. O lançamento é registrado com a data de hoje.
            """;

    private final TelegramClient client;
    private final TelegramLinkService linkService;
    private final TelegramCommandParser parser;
    private final TransactionService transactionService;

    // O Telegram reenvia o mesmo update se a resposta demorar (ex.: serviço acordando).
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
            case "/gasto", "/despesa", "/receita" -> handleTransaction(chatId, text);
            default -> client.sendMessage(chatId, "Comando não reconhecido. Use /ajuda.");
        }
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

    private void handleTransaction(long chatId, String text) {
        Optional<User> user = linkService.findUserByChatId(chatId);
        if (user.isEmpty()) {
            client.sendMessage(chatId,
                    "Sua conta ainda não está conectada. Use o botão \"Conectar Telegram\" no dashboard.");
            return;
        }

        try {
            TelegramCommandParser.ParsedTransaction parsed = parser.parse(text);

            TransactionRequestDTO dto = new TransactionRequestDTO();
            dto.setDescription(parsed.description());
            dto.setAmount(parsed.amount());
            dto.setType(parsed.type());
            dto.setDate(LocalDate.now(ZONE));

            transactionService.createForUser(user.get(), dto);

            String label = parsed.type() == TransactionType.INCOME ? "Receita" : "Despesa";
            String value = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"))
                    .format(parsed.amount());
            client.sendMessage(chatId,
                    "✅ " + label + " registrada: " + value + " — " + parsed.description());
        } catch (IllegalArgumentException e) {
            client.sendMessage(chatId, "⚠️ " + e.getMessage());
        }
    }
}
