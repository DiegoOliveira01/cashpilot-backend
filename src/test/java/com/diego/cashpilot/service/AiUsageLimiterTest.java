package com.diego.cashpilot.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiUsageLimiterTest {

    // Permite simular a virada do dia.
    private static class FakeDayLimiter extends AiUsageLimiter {
        LocalDate today = LocalDate.of(2026, 10, 22);

        FakeDayLimiter(int perUser, int global) {
            super(perUser, global);
        }

        @Override
        LocalDate today() {
            return today;
        }
    }

    @Test
    void limitsPerUserAndResetsNextDay() {
        var limiter = new FakeDayLimiter(2, 100);

        assertEquals(AiUsageLimiter.Decision.ALLOWED, limiter.tryAcquire(1));
        assertEquals(AiUsageLimiter.Decision.ALLOWED, limiter.tryAcquire(1));
        assertEquals(AiUsageLimiter.Decision.USER_LIMIT, limiter.tryAcquire(1));
        assertEquals(AiUsageLimiter.Decision.ALLOWED, limiter.tryAcquire(2)); // outro usuário não é afetado

        limiter.today = limiter.today.plusDays(1);
        assertEquals(AiUsageLimiter.Decision.ALLOWED, limiter.tryAcquire(1)); // novo dia, contador zerado
    }

    @Test
    void limitsGlobally() {
        var limiter = new FakeDayLimiter(10, 2);

        assertEquals(AiUsageLimiter.Decision.ALLOWED, limiter.tryAcquire(1));
        assertEquals(AiUsageLimiter.Decision.ALLOWED, limiter.tryAcquire(2));
        assertEquals(AiUsageLimiter.Decision.GLOBAL_LIMIT, limiter.tryAcquire(3));
    }
}
