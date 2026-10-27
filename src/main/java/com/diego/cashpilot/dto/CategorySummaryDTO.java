package com.diego.cashpilot.dto;

import com.diego.cashpilot.model.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CategorySummaryDTO {

    // null = "Sem categoria"
    private Long categoryId;
    private String name;
    private String icon;
    private TransactionType type;
    private BigDecimal total;
}
