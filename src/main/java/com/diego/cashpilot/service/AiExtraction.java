package com.diego.cashpilot.service;

import com.diego.cashpilot.model.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * O que o modelo devolveu. NUNCA é confiável: toResult() valida tudo antes de virar lançamento.
 * O nome da categoria é só uma sugestão: o bot só a usa se existir entre as categorias do usuário.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiExtraction(boolean understood, String type, BigDecimal amount, String description,
                           String date, String category) {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    public Optional<TelegramFreeTextParser.Result> toResult(LocalDate today) {
        if (!understood || type == null || amount == null) return Optional.empty();

        TransactionType parsedType;
        String t = type.trim().toUpperCase();
        if (t.equals("INCOME")) {
            parsedType = TransactionType.INCOME;
        } else if (t.equals("EXPENSE")) {
            parsedType = TransactionType.EXPENSE;
        } else {
            return Optional.empty();
        }

        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        if (value.signum() <= 0 || value.compareTo(MAX_AMOUNT) > 0) return Optional.empty();

        LocalDate parsedDate;
        if (date == null || date.isBlank()) {
            parsedDate = today;
        } else {
            try {
                parsedDate = LocalDate.parse(date.trim());
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        // Sem datas no futuro nem muito antigas: na dúvida, não registra.
        if (parsedDate.isAfter(today) || parsedDate.isBefore(today.minusYears(1))) {
            return Optional.empty();
        }

        String desc = description == null ? "" : description.trim();
        if (desc.isEmpty()) {
            desc = parsedType == TransactionType.INCOME ? "Receita" : "Despesa";
        }
        if (desc.length() > 255) desc = desc.substring(0, 255);

        String hint = category == null || category.isBlank() ? null : category.trim();

        return Optional.of(new TelegramFreeTextParser.Result(parsedType, value, desc, parsedDate, hint));
    }
}
