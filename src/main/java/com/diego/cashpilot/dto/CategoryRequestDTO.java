package com.diego.cashpilot.dto;

import com.diego.cashpilot.model.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequestDTO {

    @NotBlank
    @Size(max = 40)
    private String name;

    @NotNull
    private TransactionType type;

    @Size(max = 16)
    private String icon;
}
