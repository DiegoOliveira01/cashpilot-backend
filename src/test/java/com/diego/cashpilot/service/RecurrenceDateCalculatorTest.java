package com.diego.cashpilot.service;

import com.diego.cashpilot.model.RecurringTransaction;
import com.diego.cashpilot.model.enums.RecurrenceFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceDateCalculatorTest {

    private RecurrenceDateCalculator calculator;
    private RecurringTransaction rec;

    @BeforeEach
    void setUp() {
        calculator = new RecurrenceDateCalculator();
        rec = new RecurringTransaction();
    }

    // ==========================================
    // Testes para Frequência SEMANAL (WEEKLY)
    // ==========================================

    @Test
    @DisplayName("Deve adicionar exatamente 7 dias para recorrência semanal")
    void shouldAddSevenDaysForWeeklyFrequency() {
        rec.setFrequency(RecurrenceFrequency.WEEKLY);
        LocalDate currentDate = LocalDate.of(2026, 10, 15); // Quinta-feira

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    @DisplayName("Semanal deve funcionar na virada do mês")
    void shouldHandleEndOfMonthForWeekly() {
        rec.setFrequency(RecurrenceFrequency.WEEKLY);
        LocalDate currentDate = LocalDate.of(2026, 10, 28);

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 11, 4));
    }

    // ==========================================
    // Testes para Frequência MENSAL (MONTHLY)
    // ==========================================

    @Test
    @DisplayName("Mensal: Deve avançar para o mesmo dia no próximo mês")
    void shouldAdvanceToSameDayNextMonth() {
        rec.setFrequency(RecurrenceFrequency.MONTHLY);
        rec.setFirstDayOfMonth(15);
        LocalDate currentDate = LocalDate.of(2026, 10, 15);

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    @DisplayName("Mensal: Deve ajustar para o último dia de Fevereiro se o dia for 31")
    void shouldAdjustToLastDayOfFebruaryWhenDayIs31() {
        rec.setFrequency(RecurrenceFrequency.MONTHLY);
        rec.setFirstDayOfMonth(31);
        LocalDate currentDate = LocalDate.of(2026, 1, 31); // Janeiro tem 31

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        // 2026 não é bissexto, então Fevereiro tem 28 dias
        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("Mensal: Deve ajustar para o último dia de Fevereiro (bissexto) se o dia for 31")
    void shouldAdjustToLastDayOfLeapFebruaryWhenDayIs31() {
        rec.setFrequency(RecurrenceFrequency.MONTHLY);
        rec.setFirstDayOfMonth(31);
        LocalDate currentDate = LocalDate.of(2028, 1, 31); // 2028 é bissexto

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        // 2028 é bissexto, então Fevereiro tem 29 dias
        assertThat(nextDate).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("Mensal: Deve ajustar para dia 30 se o dia for 31 e o mês seguinte tiver apenas 30")
    void shouldAdjustToDay30WhenNextMonthHas30Days() {
        rec.setFrequency(RecurrenceFrequency.MONTHLY);
        rec.setFirstDayOfMonth(31);
        LocalDate currentDate = LocalDate.of(2026, 3, 31); // Março tem 31

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        // Abril tem 30 dias
        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    // ==========================================
    // Testes para Frequência 2x NO MÊS (TWICE_MONTHLY)
    // ==========================================

    @Test
    @DisplayName("2x no mês: Do primeiro dia deve pular para o segundo dia no mesmo mês")
    void shouldJumpToSecondDayInSameMonth() {
        rec.setFrequency(RecurrenceFrequency.TWICE_MONTHLY);
        rec.setFirstDayOfMonth(5);
        rec.setSecondDayOfMonth(20);
        LocalDate currentDate = LocalDate.of(2026, 10, 5);

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    @DisplayName("2x no mês: Do segundo dia deve pular para o primeiro dia do próximo mês")
    void shouldJumpToFirstDayOfNextMonth() {
        rec.setFrequency(RecurrenceFrequency.TWICE_MONTHLY);
        rec.setFirstDayOfMonth(5);
        rec.setSecondDayOfMonth(20);
        LocalDate currentDate = LocalDate.of(2026, 10, 20);

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("2x no mês: Deve ajustar o segundo dia se o mês não tiver esse dia")
    void shouldAdjustSecondDayIfMonthIsShorter() {
        rec.setFrequency(RecurrenceFrequency.TWICE_MONTHLY);
        rec.setFirstDayOfMonth(15);
        rec.setSecondDayOfMonth(31); // Dia 31 (ex: pedido para dia 31)
        LocalDate currentDate = LocalDate.of(2026, 3, 15); // Março tem 31

        LocalDate nextDate = calculator.calculateNextDate(rec, currentDate);

        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 3, 31));

        // Simulando próximo mês (Abril, 30 dias)
        LocalDate nextDateApril = calculator.calculateNextDate(rec, nextDate);
        assertThat(nextDateApril).isEqualTo(LocalDate.of(2026, 4, 15)); // O primeiro dia cai pro próximo mês
    }
}