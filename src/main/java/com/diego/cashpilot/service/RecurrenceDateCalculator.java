package com.diego.cashpilot.service;

import com.diego.cashpilot.model.RecurringTransaction;
import com.diego.cashpilot.model.enums.RecurrenceFrequency;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RecurrenceDateCalculator {

    public LocalDate calculateNextDate(RecurringTransaction rec, LocalDate currentDate) {
        return switch (rec.getFrequency()) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> {
                LocalDate nextMonth = currentDate.plusMonths(1);
                // Usa o dia do início como referência. Se o mês não tiver o dia (ex: 31), usa o último dia.
                int targetDay = rec.getFirstDayOfMonth() != null ? rec.getFirstDayOfMonth() : rec.getStartDate().getDayOfMonth();
                yield adjustToDay(nextMonth, targetDay);
            }
            case TWICE_MONTHLY -> {
                int currentDay = currentDate.getDayOfMonth();
                // Se estamos na primeira quinzena, pula para o segundo dia
                if (currentDay < rec.getSecondDayOfMonth()) {
                    yield adjustToDay(currentDate, rec.getSecondDayOfMonth());
                } else {
                    // Se já passou do segundo dia, pula para o primeiro dia do próximo mês
                    LocalDate nextMonth = currentDate.plusMonths(1);
                    yield adjustToDay(nextMonth, rec.getFirstDayOfMonth());
                }
            }
        };
    }

    private LocalDate adjustToDay(LocalDate date, int targetDay) {
        int maxDaysInMonth = date.lengthOfMonth();
        int safeDay = Math.min(targetDay, maxDaysInMonth);
        return date.withDayOfMonth(safeDay);
    }
}