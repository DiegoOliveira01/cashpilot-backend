package com.diego.cashpilot.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreditCardResponseDTO {

    private Long id;
    private String name;
    private String brand;
    private Integer closingDay;
    private Integer dueDay;
    private BigDecimal limitAmount;
    private String imageUrl;
    private Boolean isActive;

    // Futuramente, podemos adicionar campos calculados aqui:
    // private BigDecimal currentInvoiceAmount;
    // private BigDecimal availableLimit;
}