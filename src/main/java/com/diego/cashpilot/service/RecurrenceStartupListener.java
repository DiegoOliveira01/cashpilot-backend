package com.diego.cashpilot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j // Para gerar logs no Railway
@Component
@RequiredArgsConstructor
public class RecurrenceStartupListener {

    private final RecurrenceProcessorService recurrenceProcessorService;

    /**
     * Este método roda automaticamente assim que a aplicação Spring Boot sobe.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Iniciando verificação de contas recorrentes pendentes...");

        try {
            recurrenceProcessorService.processDueRecurrences();
            log.info("Verificação de contas recorrentes concluída com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao processar contas recorrentes na inicialização", e);
            // O try/catch evita que um erro aqui derrube a aplicação inteira na subida
        }
    }
}