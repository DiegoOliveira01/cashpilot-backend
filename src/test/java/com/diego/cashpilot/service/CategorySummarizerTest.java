package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CategorySummaryDTO;
import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategorySummarizerTest {

    private final Category food = Category.builder().id(1L).name("Alimentação").type(TransactionType.EXPENSE).icon("🍽️").build();
    private final Category transport = Category.builder().id(2L).name("Transporte").type(TransactionType.EXPENSE).icon("🚗").build();
    private final Category salary = Category.builder().id(3L).name("Salário").type(TransactionType.INCOME).icon("💼").build();

    private Transaction tx(TransactionType type, String amount, Category category) {
        return Transaction.builder().type(type).amount(new BigDecimal(amount)).category(category).build();
    }

    @Test
    void sumsByCategoryAndSortsByTotalDescending() {
        List<CategorySummaryDTO> result = CategorySummarizer.summarize(List.of(
                tx(TransactionType.EXPENSE, "40.00", food),
                tx(TransactionType.EXPENSE, "35.00", transport),
                tx(TransactionType.EXPENSE, "10.50", food),
                tx(TransactionType.INCOME, "1000.00", salary)
        ));

        assertEquals(3, result.size());
        assertEquals("Salário", result.get(0).getName());
        assertEquals(new BigDecimal("1000.00"), result.get(0).getTotal());
        assertEquals("Alimentação", result.get(1).getName());
        assertEquals(new BigDecimal("50.50"), result.get(1).getTotal());
        assertEquals("Transporte", result.get(2).getName());
    }

    @Test
    void groupsTransactionsWithoutCategoryTogether() {
        List<CategorySummaryDTO> result = CategorySummarizer.summarize(List.of(
                tx(TransactionType.EXPENSE, "20.00", null),
                tx(TransactionType.EXPENSE, "5.00", null)
        ));

        assertEquals(1, result.size());
        assertNull(result.get(0).getCategoryId());
        assertEquals("Sem categoria", result.get(0).getName());
        assertEquals(new BigDecimal("25.00"), result.get(0).getTotal());
    }

    @Test
    void emptyListGivesEmptySummary() {
        assertTrue(CategorySummarizer.summarize(List.of()).isEmpty());
    }
}
