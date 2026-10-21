package com.diego.cashpilot.service;

import com.diego.cashpilot.model.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TelegramCommandParser {

    public record ParsedTransaction(TransactionType type, BigDecimal amount, String description) {}

    /**
     * Formatos aceitos:
     *   /gasto 35,90 mercado
     *   /receita 1500 salário
     * Lança IllegalArgumentException com mensagem pronta para o usuário.
     */
    public ParsedTransaction parse(String text) {
        String[] parts = text.trim().split("\\s+", 3);
        String command = parts[0].toLowerCase().replaceAll("@.*$", "");

        TransactionType type = command.equals("/receita")
                ? TransactionType.INCOME
                : TransactionType.EXPENSE;

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                    "Informe o valor. Exemplo: "
                            + (type == TransactionType.INCOME ? "/receita 1500 salário" : "/gasto 35,90 mercado"));
        }

        BigDecimal amount = parseAmount(parts[1]);

        String description = parts.length == 3 ? parts[2].trim() : "";
        if (description.isEmpty()) {
            description = type == TransactionType.INCOME ? "Receita" : "Despesa";
        }
        if (description.length() > 255) {
            description = description.substring(0, 255);
        }

        return new ParsedTransaction(type, amount, description);
    }

    private BigDecimal parseAmount(String raw) {
        String value = raw.replace("R$", "").trim();
        // "1.500,50" -> "1500.50" | "35,90" -> "35.90" | "35.90" continua igual
        String normalized = value.contains(",")
                ? value.replace(".", "").replace(",", ".")
                : value;
        try {
            BigDecimal amount = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("O valor precisa ser maior que zero.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: " + raw + ". Exemplo: 35,90");
        }
    }
}
