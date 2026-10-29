package com.diego.cashpilot.config;

import com.diego.cashpilot.model.enums.TransactionType;

import java.util.List;

/** Categorias padrão do sistema, criadas na subida da aplicação. */
public final class DefaultCategories {

    public record Item(String name, TransactionType type, String icon) {}

    public static final List<Item> ALL = List.of(
            // Despesas
            new Item("Alimentação", TransactionType.EXPENSE, "lucideUtensils"),
            new Item("Moradia", TransactionType.EXPENSE, "lucideHome"),
            new Item("Contas e serviços", TransactionType.EXPENSE, "lucideZap"),
            new Item("Transporte", TransactionType.EXPENSE, "lucideCar"),
            new Item("Saúde", TransactionType.EXPENSE, "lucideHeartPulse"),
            new Item("Lazer", TransactionType.EXPENSE, "lucideGamepad2"),
            new Item("Educação", TransactionType.EXPENSE, "lucideGraduationCap"),
            new Item("Compras", TransactionType.EXPENSE, "lucideShoppingCart"),
            new Item("Outros", TransactionType.EXPENSE, "lucideTag"),

            // Receitas
            new Item("Salário", TransactionType.INCOME, "lucideBriefcase"),
            new Item("Freelance", TransactionType.INCOME, "lucideLaptop"), // Novo ícone
            new Item("Investimentos", TransactionType.INCOME, "lucidePiggyBank"),
            new Item("Presente", TransactionType.INCOME, "lucideGift"),
            new Item("Outros", TransactionType.INCOME, "lucideTag")
    );

    private DefaultCategories() {}
}
