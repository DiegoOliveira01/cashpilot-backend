package com.diego.cashpilot.service;

import com.diego.cashpilot.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O modelo pode devolver qualquer coisa: o que não passar na validação não vira lançamento. */
class AiExtractionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private AiExtraction of(boolean understood, String type, String amount, String desc, String date) {
        return new AiExtraction(understood, type, amount == null ? null : new BigDecimal(amount), desc, date);
    }

    @Test
    void acceptsValidExtraction() {
        var r = of(true, "EXPENSE", "49.5", " mercado ", "2026-10-21").toResult(TODAY).orElseThrow();

        assertEquals(TransactionType.EXPENSE, r.type());
        assertEquals(new BigDecimal("49.50"), r.amount());
        assertEquals("mercado", r.description());
        assertEquals(LocalDate.of(2026, 10, 21), r.date());
    }

    @Test
    void usesTodayAndDefaultDescriptionWhenMissing() {
        var r = of(true, "income", "200", " ", null).toResult(TODAY).orElseThrow();

        assertEquals(TransactionType.INCOME, r.type());
        assertEquals("Receita", r.description());
        assertEquals(TODAY, r.date());
    }

    @Test
    void rejectsWhatIsNotSafe() {
        assertTrue(of(false, "EXPENSE", "10", "x", null).toResult(TODAY).isEmpty());        // não entendeu
        assertTrue(of(true, "TRANSFER", "10", "x", null).toResult(TODAY).isEmpty());        // tipo inválido
        assertTrue(of(true, "EXPENSE", "0", "x", null).toResult(TODAY).isEmpty());          // valor zero
        assertTrue(of(true, "EXPENSE", "-5", "x", null).toResult(TODAY).isEmpty());         // valor negativo
        assertTrue(of(true, "EXPENSE", "99999999", "x", null).toResult(TODAY).isEmpty());   // valor absurdo
        assertTrue(of(true, "EXPENSE", null, "x", null).toResult(TODAY).isEmpty());         // sem valor
        assertTrue(of(true, "EXPENSE", "10", "x", "ontem").toResult(TODAY).isEmpty());      // data inválida
        assertTrue(of(true, "EXPENSE", "10", "x", "2026-12-25").toResult(TODAY).isEmpty()); // futuro
        assertTrue(of(true, "EXPENSE", "10", "x", "2020-01-01").toResult(TODAY).isEmpty()); // muito antiga
    }
}
