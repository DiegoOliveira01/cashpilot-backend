package com.diego.cashpilot.service;

import com.diego.cashpilot.model.enums.TransactionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramFreeTextParserTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private final TelegramFreeTextParser parser = new TelegramFreeTextParser();

    static Stream<Arguments> understood() {
        return Stream.of(
                Arguments.of("gastei 40 no mercado", TransactionType.EXPENSE, "40.00", "mercado", TODAY),
                Arguments.of("paguei 120 de luz ontem", TransactionType.EXPENSE, "120.00", "luz", LocalDate.of(2026, 10, 21)),
                Arguments.of("recebi 1.500 de salário", TransactionType.INCOME, "1500.00", "salário", TODAY),
                Arguments.of("ganhei 50 do João dia 15", TransactionType.INCOME, "50.00", "João", LocalDate.of(2026, 10, 15)),
                Arguments.of("comprei pão R$ 12,50 hoje", TransactionType.EXPENSE, "12.50", "pão", TODAY),
                Arguments.of("gastei 35,90 no ifood anteontem", TransactionType.EXPENSE, "35.90", "ifood", LocalDate.of(2026, 10, 20)),
                Arguments.of("paguei 2 parcelas de R$ 150", TransactionType.EXPENSE, "150.00", "2 parcelas", TODAY),
                Arguments.of("gastei 80 reais com gasolina 20/10", TransactionType.EXPENSE, "80.00", "gasolina", LocalDate.of(2026, 10, 20)),
                Arguments.of("gastei 1.500,50 no aluguel", TransactionType.EXPENSE, "1500.50", "aluguel", TODAY),
                Arguments.of("gastei 35.90 na farmácia", TransactionType.EXPENSE, "35.90", "farmácia", TODAY),
                Arguments.of("gastei 25 dia 30", TransactionType.EXPENSE, "25.00", "Despesa", LocalDate.of(2026, 9, 30)),
                Arguments.of("gastei 40", TransactionType.EXPENSE, "40.00", "Despesa", TODAY),
                Arguments.of("recebi 200", TransactionType.INCOME, "200.00", "Receita", TODAY)
        );
    }

    @ParameterizedTest(name = "\"{0}\"")
    @MethodSource("understood")
    void parsesFreeText(String text, TransactionType type, String amount, String description, LocalDate date) {
        var result = parser.parse(text, TODAY)
                .orElseThrow(() -> new AssertionError("Não entendeu: " + text));

        assertEquals(type, result.type());
        assertEquals(new BigDecimal(amount), result.amount());
        assertEquals(description, result.description());
        assertEquals(date, result.date());
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "uber 23,50",                       // sem verbo: ambíguo
            "comprei 3 camisetas por 90",       // dois números sem marcador
            "vendi e gastei 50",                // receita e despesa na mesma frase
            "gastei muito hoje",                // sem valor
            "gastei 0",                         // valor zero
            "gastei 40 dia 31",                 // dia inexistente no mês anterior
            "gastei 40 25/12",                  // data no futuro
            "oi",
            "/gasto 40 mercado"                 // comandos são tratados em outro lugar
    })
    void returnsEmptyWhenNotSure(String text) {
        assertTrue(parser.parse(text, TODAY).isEmpty(), "Deveria ser ambíguo: " + text);
    }
}
