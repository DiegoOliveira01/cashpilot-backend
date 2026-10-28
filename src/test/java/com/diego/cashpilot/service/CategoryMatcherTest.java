package com.diego.cashpilot.service;

import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMatcherTest {

    private static final TransactionType EXPENSE = TransactionType.EXPENSE;
    private static final TransactionType INCOME = TransactionType.INCOME;

    private long nextId = 1;

    private Category defaultCategory(String name, TransactionType type) {
        return Category.builder().id(nextId++).name(name).type(type).build();
    }

    private Category ownCategory(String name, TransactionType type) {
        return Category.builder().id(nextId++).name(name).type(type).user(new User()).build();
    }

    private List<Category> categories() {
        return List.of(
                defaultCategory("Alimentação", EXPENSE), defaultCategory("Moradia", EXPENSE),
                defaultCategory("Contas e serviços", EXPENSE), defaultCategory("Transporte", EXPENSE),
                defaultCategory("Saúde", EXPENSE), defaultCategory("Lazer", EXPENSE),
                defaultCategory("Educação", EXPENSE), defaultCategory("Compras", EXPENSE),
                defaultCategory("Outros", EXPENSE),
                defaultCategory("Salário", INCOME), defaultCategory("Freelance", INCOME),
                defaultCategory("Investimentos", INCOME), defaultCategory("Presente", INCOME),
                defaultCategory("Outros", INCOME),
                ownCategory("Pets", EXPENSE));
    }

    private String match(String description, TransactionType type) {
        return CategoryMatcher.match(description, type, categories()).map(Category::getName).orElse(null);
    }

    private String find(String query, TransactionType type) {
        return CategoryMatcher.findByName(query, type, categories()).map(Category::getName).orElse(null);
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({
            "mercado, Alimentação",
            "supermercado, Alimentação",
            "Almoço, Alimentação",
            "uber, Transporte",
            "Uber, Transporte",
            "conta de luz, Contas e serviços",
            "internet, Contas e serviços",
            "aluguel, Moradia",
            "farmácia, Saúde",
            "netflix, Lazer",
            "mercado livre, Compras",
            "ração pets, Pets",
            "curso de inglês, Educação",
            "gasolina, Transporte"
    })
    void matchesExpenseByDescription(String description, String expected) {
        assertEquals(expected, match(description, EXPENSE));
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({
            "salário, Salário",
            "holerite, Salário",
            "pagamento do projeto, Freelance",
            "dividendos, Investimentos",
            "mesada, Presente"
    })
    void matchesIncomeByDescription(String description, String expected) {
        assertEquals(expected, match(description, INCOME));
    }

    @Test
    void doesNotGuessWhenThereIsNoClue() {
        assertTrue(CategoryMatcher.match("xyz", EXPENSE, categories()).isEmpty());
        assertTrue(CategoryMatcher.match("   ", EXPENSE, categories()).isEmpty());
    }

    @Test
    void respectsTheTransactionType() {
        assertTrue(CategoryMatcher.match("salário", EXPENSE, categories()).isEmpty());
    }

    @Test
    void othersIsNeverChosenAutomatically() {
        assertTrue(CategoryMatcher.match("outros", EXPENSE, categories()).isEmpty());
    }

    @Test
    void wholeWordsOnly() {
        // "gas" é palavra-chave de Contas e serviços, mas não casa dentro de "gasolina"/"gastronomia"
        assertEquals("Transporte", match("gasolina", EXPENSE));
        assertTrue(CategoryMatcher.match("gastronomia", EXPENSE, categories()).isEmpty());
    }

    @ParameterizedTest(name = "#{0} → {1}")
    @CsvSource({
            "lazer, Lazer",
            "Alimentação, Alimentação",
            "alim, Alimentação",
            "contas, Contas e serviços",
            "servicos, Contas e serviços",
            "contas_e_servicos, Contas e serviços",
            "pets, Pets"
    })
    void findsByNameInFlexibleWays(String query, String expected) {
        assertEquals(expected, find(query, EXPENSE));
    }

    @Test
    void findByNameDoesNotGuessWhenAmbiguousOrTooShort() {
        assertTrue(CategoryMatcher.findByName("o", EXPENSE, categories()).isEmpty());
        assertTrue(CategoryMatcher.findByName("inexistente", EXPENSE, categories()).isEmpty());
        // "Outros" existe nos dois tipos: só o tipo pedido é considerado
        Optional<Category> others = CategoryMatcher.findByName("outros", INCOME, categories());
        assertEquals(INCOME, others.orElseThrow().getType());
    }
}
