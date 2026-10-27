package com.diego.cashpilot.dto;

import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponseDTO {

    private Long id;
    private String name;
    private TransactionType type;
    private String icon;

    // true = categoria padrão do sistema (não pode ser editada nem apagada)
    private boolean builtIn;

    public static CategoryResponseDTO of(Category category) {
        if (category == null) return null;

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .builtIn(category.getUser() == null)
                .build();
    }
}
