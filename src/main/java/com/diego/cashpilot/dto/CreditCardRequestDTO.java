package com.diego.cashpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditCardRequestDTO {

    @NotBlank(message = "O nome do cartão é obrigatório")
    private String name;

    @NotBlank(message = "A bandeira é obrigatória")
    private String brand;

    @NotNull(message = "O dia de fechamento é obrigatório")
    private Integer closingDay;

    @NotNull(message = "O dia de vencimento é obrigatório")
    private Integer dueDay;

    @NotNull(message = "O limite é obrigatório")
    @Positive(message = "O limite deve ser maior que zero")
    private BigDecimal limitAmount;

    @NotBlank(message = "A imagem do cartão é obrigatória")
    private String imageUrl;
}