package com.diego.cashpilot.dto;

import com.diego.cashpilot.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequestDTO {

    @NotNull
    private String description;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    private Long categoryId; // opcional

    @NotNull
    private LocalDate date;

}
