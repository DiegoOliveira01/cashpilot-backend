package com.diego.cashpilot.dto;

import com.diego.cashpilot.model.enums.RecurrenceFrequency;
import com.diego.cashpilot.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponseDTO(
        Long id,
        String description,
        BigDecimal amount,
        TransactionType type,
        Long categoryId,
        String categoryName,
        String categoryIcon,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate nextOccurrenceDate,
        Boolean isActive
) {}