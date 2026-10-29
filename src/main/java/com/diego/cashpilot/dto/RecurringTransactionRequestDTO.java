package com.diego.cashpilot.dto;

import com.diego.cashpilot.model.enums.RecurrenceFrequency;
import com.diego.cashpilot.model.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequestDTO(
        @NotBlank(message = "A descrição é obrigatória")
        String description,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal amount,

        @NotNull(message = "O tipo é obrigatório")
        TransactionType type,

        Long categoryId, // Pode ser nulo (Sem categoria)

        @NotNull(message = "A frequência é obrigatória")
        RecurrenceFrequency frequency,

        Integer firstDayOfMonth,  // Usado para MONTHLY e TWICE_MONTHLY
        Integer secondDayOfMonth, // Usado para TWICE_MONTHLY

        @NotNull(message = "A data de início é obrigatória")
        LocalDate startDate,

        LocalDate endDate,        // Opcional
        Integer maxOccurrences    // Opcional
) {}