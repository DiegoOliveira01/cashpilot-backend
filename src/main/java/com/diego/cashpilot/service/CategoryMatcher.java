package com.diego.cashpilot.service;

import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.enums.TransactionType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Descobre a categoria de um lançamento (lógica pura, sem banco e sem IA):
 *  - match(): pela descrição, usando o nome da categoria e palavras-chave das categorias padrão;
 *  - findByName(): por um nome digitado pelo usuário (#tag, /categoria) ou devolvido pela IA.
 * Na dúvida, devolve vazio: é melhor ficar "sem categoria" do que classificar errado.
 */
public final class CategoryMatcher {

    private static final int MIN_PREFIX_LENGTH = 3;
    private static final String OTHERS = "outros";

    // Chaves = nome normalizado da categoria padrão (sem acento, minúsculo).
    private static final Map<String, List<String>> EXPENSE_KEYWORDS = Map.of(
            "alimentacao", List.of("mercado", "supermercado", "feira", "padaria", "restaurante", "lanche",
                    "lanchonete", "pizza", "pizzaria", "hamburguer", "burger", "ifood", "rappi", "delivery",
                    "almoco", "jantar", "janta", "cafe", "cafeteria", "sorvete", "acai", "churrasco", "comida",
                    "acougue", "hortifruti", "marmita", "sacolao"),
            "moradia", List.of("aluguel", "condominio", "iptu", "financiamento", "reforma", "movel", "moveis",
                    "decoracao"),
            "contas e servicos", List.of("luz", "energia", "agua", "internet", "telefone", "gas", "esgoto",
                    "seguro", "assinatura", "conta de luz", "conta de agua"),
            "transporte", List.of("uber", "99", "taxi", "onibus", "metro", "passagem", "gasolina", "combustivel",
                    "etanol", "estacionamento", "pedagio", "ipva", "oficina", "mecanico", "trem", "bilhete unico"),
            "saude", List.of("farmacia", "remedio", "medico", "consulta", "exame", "dentista", "hospital",
                    "plano de saude", "academia", "psicologo", "terapia", "vacina"),
            "lazer", List.of("cinema", "netflix", "spotify", "show", "ingresso", "viagem", "hotel", "bar",
                    "balada", "jogo", "game", "steam", "playstation", "xbox", "streaming", "youtube", "disney",
                    "hbo", "prime", "festa", "parque"),
            "educacao", List.of("curso", "faculdade", "escola", "mensalidade", "livro", "livraria", "apostila",
                    "udemy", "alura", "matricula", "ingles", "idioma"),
            "compras", List.of("roupa", "camisa", "camiseta", "calca", "sapato", "tenis", "shopping", "loja",
                    "amazon", "shopee", "mercado livre", "magalu")
    );

    private static final Map<String, List<String>> INCOME_KEYWORDS = Map.of(
            "salario", List.of("holerite", "contracheque", "decimo terceiro", "ferias", "adiantamento"),
            "freelance", List.of("freela", "bico", "projeto", "cliente", "consultoria"),
            "investimentos", List.of("dividendo", "rendimento", "juros", "cdb", "tesouro", "acao", "acoes",
                    "fii", "cripto", "bitcoin", "lucro"),
            "presente", List.of("mesada", "doacao", "aniversario")
    );

    private static final Map<TransactionType, Map<String, List<String>>> KEYWORDS = Map.of(
            TransactionType.EXPENSE, EXPENSE_KEYWORDS,
            TransactionType.INCOME, INCOME_KEYWORDS
    );

    private CategoryMatcher() {}

    /** Categoria pela descrição. Vence o termo mais longo ("mercado livre" ganha de "mercado"). */
    public static Optional<Category> match(String description, TransactionType type, List<Category> available) {
        String text = normalize(description);
        if (text.isEmpty()) return Optional.empty();

        Map<String, List<String>> keywords = KEYWORDS.getOrDefault(type, Map.of());

        // As categorias do próprio usuário vêm primeiro (desempate a favor delas); "Outros" nunca é automática.
        List<Category> candidates = available.stream()
                .filter(c -> c.getType() == type && !OTHERS.equals(normalize(c.getName())))
                .sorted(Comparator.comparing((Category c) -> c.getUser() == null))
                .toList();

        Category best = null;
        int bestLength = 0;

        for (Category category : candidates) {
            String name = normalize(category.getName());

            List<String> terms = new ArrayList<>();
            terms.add(name);
            if (category.getUser() == null) {
                terms.addAll(keywords.getOrDefault(name, List.of()));
            }

            for (String term : terms) {
                if (term.length() > bestLength && containsTerm(text, term)) {
                    best = category;
                    bestLength = term.length();
                }
            }
        }

        return Optional.ofNullable(best);
    }

    /**
     * Categoria por nome: exato; senão prefixo do nome ("alim" → Alimentação); senão início de
     * uma palavra do nome ("servicos" → Contas e serviços). Só aceita se houver uma única opção.
     */
    public static Optional<Category> findByName(String query, TransactionType type, List<Category> available) {
        String q = normalize(query);
        if (q.isEmpty()) return Optional.empty();

        List<Category> sameType = available.stream().filter(c -> c.getType() == type).toList();

        List<Category> exact = sameType.stream().filter(c -> normalize(c.getName()).equals(q)).toList();
        if (exact.size() == 1) return Optional.of(exact.get(0));
        if (q.length() < MIN_PREFIX_LENGTH) return Optional.empty();

        List<Category> byPrefix = sameType.stream().filter(c -> normalize(c.getName()).startsWith(q)).toList();
        if (byPrefix.size() == 1) return Optional.of(byPrefix.get(0));
        if (!byPrefix.isEmpty()) return Optional.empty(); // ambíguo: não chuta

        List<Category> byWord = sameType.stream()
                .filter(c -> Arrays.stream(normalize(c.getName()).split(" ")).anyMatch(w -> w.startsWith(q)))
                .toList();
        return byWord.size() == 1 ? Optional.of(byWord.get(0)) : Optional.empty();
    }

    // Palavra inteira (com plural simples): "mercado" não casa com "supermercado".
    private static boolean containsTerm(String text, String term) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?:s|es)?(?![a-z0-9])")
                .matcher(text)
                .find();
    }

    /** Sem acento, minúsculo e só letras/números separados por espaço ("contas_e-servicos" → "contas e servicos"). */
    static String normalize(String value) {
        if (value == null) return "";
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
