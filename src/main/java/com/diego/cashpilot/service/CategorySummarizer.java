package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CategorySummaryDTO;
import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Soma as transações por categoria e tipo (lógica pura, fácil de testar). */
public final class CategorySummarizer {

    static final String NO_CATEGORY_NAME = "Sem categoria";
    static final String NO_CATEGORY_ICON = "❔";

    private record Key(Long categoryId, TransactionType type) {}

    private CategorySummarizer() {}

    public static List<CategorySummaryDTO> summarize(List<Transaction> transactions) {
        Map<Key, CategorySummaryDTO> byKey = new LinkedHashMap<>();

        for (Transaction t : transactions) {
            Category category = t.getCategory();
            Key key = new Key(category == null ? null : category.getId(), t.getType());

            CategorySummaryDTO current = byKey.get(key);
            BigDecimal total = (current == null ? BigDecimal.ZERO : current.getTotal()).add(t.getAmount());

            byKey.put(key, CategorySummaryDTO.builder()
                    .categoryId(category == null ? null : category.getId())
                    .name(category == null ? NO_CATEGORY_NAME : category.getName())
                    .icon(category == null ? NO_CATEGORY_ICON : category.getIcon())
                    .type(t.getType())
                    .total(total)
                    .build());
        }

        return byKey.values().stream()
                .sorted(Comparator.comparing(CategorySummaryDTO::getTotal).reversed())
                .toList();
    }
}
