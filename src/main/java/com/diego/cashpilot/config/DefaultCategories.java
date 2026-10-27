package com.diego.cashpilot.config;

import com.diego.cashpilot.model.enums.TransactionType;

import java.util.List;

/** Categorias padrão do sistema, criadas na subida da aplicação. */
public final class DefaultCategories {

    public record Item(String name, TransactionType type, String icon) {}

    public static final List<Item> ALL = List.of(
            // Despesas
            new Item("Alimentação", TransactionType.EXPENSE, "🍽️"),
            new Item("Moradia", TransactionType.EXPENSE, "🏠"),
            new Item("Contas e serviços", TransactionType.EXPENSE, "💡"),
            new Item("Transporte", TransactionType.EXPENSE, "🚗"),
            new Item("Saúde", TransactionType.EXPENSE, "💊"),
            new Item("Lazer", TransactionType.EXPENSE, "🎮"),
            new Item("Educação", TransactionType.EXPENSE, "📚"),
            new Item("Compras", TransactionType.EXPENSE, "🛍️"),
            new Item("Outros", TransactionType.EXPENSE, "📦"),
            // Receitas
            new Item("Salário", TransactionType.INCOME, "💼"),
            new Item("Freelance", TransactionType.INCOME, "💻"),
            new Item("Investimentos", TransactionType.INCOME, "📈"),
            new Item("Presente", TransactionType.INCOME, "🎁"),
            new Item("Outros", TransactionType.INCOME, "📦")
    );

    private DefaultCategories() {}
}
