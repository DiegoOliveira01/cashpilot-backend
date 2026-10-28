package com.diego.cashpilot.service;

import com.diego.cashpilot.model.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta frases soltas como "gastei 40 no mercado ontem" ou "recebi 1.500 de salário".
 * Sem LLM e sem chamadas externas. Se não tiver certeza, devolve vazio (nunca chuta).
 */
@Component
public class TelegramFreeTextParser {

    // categoryHint: nome de categoria sugerido pela IA (null quando o parser por regras interpreta).
    public record Result(TransactionType type, BigDecimal amount, String description, LocalDate date,
                         String categoryHint) {

        public Result(TransactionType type, BigDecimal amount, String description, LocalDate date) {
            this(type, amount, description, date, null);
        }
    }

    private record Candidate(int index, BigDecimal value, boolean marked) {}

    private static final int MAX_LENGTH = 200;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    private static final Set<String> EXPENSE_WORDS = Set.of(
            "gastei", "gasto", "paguei", "pago", "comprei", "saiu", "debitou", "custou", "despesa");
    private static final Set<String> INCOME_WORDS = Set.of(
            "recebi", "ganhei", "entrou", "caiu", "vendi", "receita", "recebimento");
    private static final Set<String> CURRENCY_WORDS = Set.of(
            "reais", "real", "conto", "contos", "pila", "pilas");
    private static final Set<String> STOPWORDS = Set.of(
            "de", "do", "da", "dos", "das", "no", "na", "nos", "nas", "em", "com", "o", "a", "os", "as",
            "um", "uma", "uns", "umas", "por", "pra", "para", "que", "foi", "meu", "minha",
            "ao", "aos", "fui", "passei", "vim");
    // "e"/"ou" sobrando nas pontas da descrição ("fui ao mercado e gastei 50" -> "mercado").
    private static final Set<String> EDGE_CONNECTORS = Set.of("e", "ou");

    // "1.500,50" | "1.500" (milhar) | "35,90" | "35.90" | "40"
    private static final Pattern NUMBER =
            Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?");
    private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+");
    private static final Pattern DATE_SLASH = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?");

    public Optional<Result> parse(String text, LocalDate today) {
        if (text == null) return Optional.empty();
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH || trimmed.startsWith("/")) {
            return Optional.empty();
        }

        String[] tokens = trimmed.split("\\s+");
        int n = tokens.length;
        String[] norm = new String[n];
        for (int i = 0; i < n; i++) norm[i] = normalize(tokens[i]);
        boolean[] consumed = new boolean[n];

        // 1) Tipo: precisa de um verbo claro; nenhum ou os dois tipos = ambíguo.
        boolean expense = false;
        boolean income = false;
        for (int i = 0; i < n; i++) {
            if (EXPENSE_WORDS.contains(norm[i])) {
                expense = true;
                consumed[i] = true;
            } else if (INCOME_WORDS.contains(norm[i])) {
                income = true;
                consumed[i] = true;
            }
        }
        if (expense == income) return Optional.empty();
        TransactionType type = income ? TransactionType.INCOME : TransactionType.EXPENSE;

        // 2) Data (antes do valor, para "dia 15" e "15/10" não virarem valor).
        LocalDate date = null;
        for (int i = 0; i < n && date == null; i++) {
            String w = norm[i];
            if (w.equals("hoje")) {
                date = today;
                consumed[i] = true;
            } else if (w.equals("ontem")) {
                date = today.minusDays(1);
                consumed[i] = true;
            } else if (w.equals("anteontem")) {
                date = today.minusDays(2);
                consumed[i] = true;
            } else if (w.equals("dia") && i + 1 < n && norm[i + 1].matches("\\d{1,2}")) {
                Optional<LocalDate> d = dayOfMonth(Integer.parseInt(norm[i + 1]), today);
                if (d.isEmpty()) return Optional.empty();
                date = d.get();
                consumed[i] = true;
                consumed[i + 1] = true;
            } else {
                Matcher m = DATE_SLASH.matcher(w);
                if (m.matches()) {
                    Optional<LocalDate> d = explicitDate(m, today);
                    if (d.isEmpty()) return Optional.empty();
                    date = d.get();
                    consumed[i] = true;
                }
            }
        }
        if (date == null) date = today;

        // 3) Valor: prefere o número com "R$" ou "reais"; sem marcador, só aceita se houver um único número.
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (consumed[i]) continue;
            String w = norm[i];
            boolean marked = false;
            if (w.startsWith("r$")) {
                w = w.substring(2);
                marked = true;
            }
            if (w.isEmpty() || !NUMBER.matcher(w).matches()) continue;
            if (i > 0 && norm[i - 1].equals("r$")) marked = true;
            if (i + 1 < n && CURRENCY_WORDS.contains(norm[i + 1])) marked = true;
            candidates.add(new Candidate(i, toAmount(w), marked));
        }

        List<Candidate> marked = candidates.stream().filter(Candidate::marked).toList();
        Candidate chosen;
        if (marked.size() == 1) {
            chosen = marked.get(0);
        } else if (marked.isEmpty() && candidates.size() == 1) {
            chosen = candidates.get(0);
        } else {
            return Optional.empty(); // nenhum número ou vários: ambíguo
        }
        if (chosen.value().signum() <= 0 || chosen.value().compareTo(MAX_AMOUNT) > 0) {
            return Optional.empty();
        }

        int idx = chosen.index();
        consumed[idx] = true;
        if (idx > 0 && norm[idx - 1].equals("r$")) consumed[idx - 1] = true;
        if (idx + 1 < n && CURRENCY_WORDS.contains(norm[idx + 1])) consumed[idx + 1] = true;

        // 4) Descrição: o que sobrou, sem conectores.
        List<String> words = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (consumed[i] || norm[i].equals("r$") || STOPWORDS.contains(norm[i])) continue;
            String word = tokens[i].replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
            if (!word.isEmpty()) words.add(word);
        }
        while (!words.isEmpty() && EDGE_CONNECTORS.contains(normalize(words.get(0)))) words.remove(0);
        while (!words.isEmpty() && EDGE_CONNECTORS.contains(normalize(words.get(words.size() - 1)))) {
            words.remove(words.size() - 1);
        }

        String finalDescription = words.isEmpty()
                ? (type == TransactionType.INCOME ? "Receita" : "Despesa")
                : String.join(" ", words);
        if (finalDescription.length() > 255) finalDescription = finalDescription.substring(0, 255);

        return Optional.of(new Result(type, chosen.value(), finalDescription, date));
    }

    /** Sem acento, minúsculas e sem pontuação nas pontas (mantém o "$" de "R$"). */
    private String normalize(String token) {
        String noAccents = Normalizer.normalize(token, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase().replaceAll("^[^a-z0-9$]+|[^a-z0-9$]+$", "");
    }

    private BigDecimal toAmount(String w) {
        String normalized;
        if (w.contains(",")) {
            normalized = w.replace(".", "").replace(",", ".");   // 1.500,50 -> 1500.50
        } else if (THOUSANDS.matcher(w).matches()) {
            normalized = w.replace(".", "");                      // 1.500 -> 1500
        } else {
            normalized = w;                                       // 35.90 -> 35.90
        }
        return new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
    }

    // "dia 15": se o dia ainda não chegou neste mês, assume o mês anterior.
    private Optional<LocalDate> dayOfMonth(int day, LocalDate today) {
        YearMonth ym = day <= today.getDayOfMonth()
                ? YearMonth.from(today)
                : YearMonth.from(today).minusMonths(1);
        if (day < 1 || day > ym.lengthOfMonth()) return Optional.empty();
        return Optional.of(ym.atDay(day));
    }

    // "15/10" ou "15/10/2026"; datas futuras são rejeitadas.
    private Optional<LocalDate> explicitDate(Matcher m, LocalDate today) {
        try {
            int day = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int year = today.getYear();
            if (m.group(3) != null) {
                year = Integer.parseInt(m.group(3));
                if (m.group(3).length() == 2) year += 2000;
            }
            LocalDate date = LocalDate.of(year, month, day);
            return date.isAfter(today) ? Optional.empty() : Optional.of(date);
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }
}
