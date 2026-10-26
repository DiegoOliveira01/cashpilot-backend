package com.diego.cashpilot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Limite diário de chamadas à IA: por usuário e global (abaixo da cota gratuita do Gemini).
 * Fica em memória: se o serviço reiniciar, os contadores zeram. O teto de verdade é a cota do provedor.
 */
@Component
public class AiUsageLimiter {

    public enum Decision { ALLOWED, USER_LIMIT, GLOBAL_LIMIT }

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final int perUserLimit;
    private final int globalLimit;

    private LocalDate day = LocalDate.MIN;
    private int globalCount = 0;
    private final Map<Long, Integer> perUser = new HashMap<>();

    public AiUsageLimiter(@Value("${gemini.daily-limit-per-user:20}") int perUserLimit,
                          @Value("${gemini.daily-limit-global:200}") int globalLimit) {
        this.perUserLimit = perUserLimit;
        this.globalLimit = globalLimit;
    }

    /** Reserva uma chamada. Conta mesmo que a chamada falhe depois (conservador). */
    public synchronized Decision tryAcquire(long userId) {
        LocalDate today = today();
        if (!today.equals(day)) {
            day = today;
            globalCount = 0;
            perUser.clear();
        }

        if (globalCount >= globalLimit) return Decision.GLOBAL_LIMIT;

        int used = perUser.getOrDefault(userId, 0);
        if (used >= perUserLimit) return Decision.USER_LIMIT;

        perUser.put(userId, used + 1);
        globalCount++;
        return Decision.ALLOWED;
    }

    // Separado para os testes poderem simular a virada do dia.
    LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
