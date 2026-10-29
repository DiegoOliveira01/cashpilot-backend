package com.diego.cashpilot.service;

import com.diego.cashpilot.model.RecurringTransaction;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.repository.RecurringTransactionRepository;
import com.diego.cashpilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurrenceProcessorService {

    private final RecurringTransactionRepository recurringRepo;
    private final TransactionRepository transactionRepo;
    private final RecurrenceDateCalculator dateCalculator;

    /**
     * Varre o banco em busca de recorrências vencidas e gera as transações devidas.
     * É anotado com @Transactional para garantir que se uma falhar, todas dão rollback.
     */
    @Transactional
    public void processDueRecurrences() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> activeRecurrences = recurringRepo.findByIsActiveTrueAndNextOccurrenceDateLessThanEqual(today);

        for (RecurringTransaction rec : activeRecurrences) {
            boolean shouldDeactivate = false;

            // Loop para gerar todas as ocorrências atrasadas (ex: usuário ficou 2 meses sem abrir o app)
            while (!rec.getNextOccurrenceDate().isAfter(today)) {

                // 1. Verifica no banco se já não foi gerada (Segurança contra concorrência)
                boolean alreadyExists = transactionRepo.existsByRecurringTransactionIdAndRecurrenceDate(
                        rec.getId(), rec.getNextOccurrenceDate());

                if (!alreadyExists) {
                    // 2. Cria a transação real
                    Transaction t = new Transaction();
                    t.setDescription(rec.getDescription());
                    t.setAmount(rec.getAmount());
                    t.setType(rec.getType());
                    t.setCategory(rec.getCategory());
                    t.setDate(rec.getNextOccurrenceDate()); // A data da transação é a data da ocorrência
                    t.setRecurringTransaction(rec);
                    t.setRecurrenceDate(rec.getNextOccurrenceDate());

                    transactionRepo.save(t);
                    log.info("Transação recorrente gerada: {} para {}", rec.getDescription(), rec.getNextOccurrenceDate());
                }

                // 3. Atualiza os ponteiros do molde
                rec.setOccurrencesGenerated(rec.getOccurrencesGenerated() + 1);
                rec.setNextOccurrenceDate(dateCalculator.calculateNextDate(rec, rec.getNextOccurrenceDate()));

                // 4. Verifica condições de parada
                if (rec.getEndDate() != null && rec.getNextOccurrenceDate().isAfter(rec.getEndDate())) {
                    shouldDeactivate = true;
                    break;
                }
                if (rec.getMaxOccurrences() != null && rec.getOccurrencesGenerated() >= rec.getMaxOccurrences()) {
                    shouldDeactivate = true;
                    break;
                }
            }

            if (shouldDeactivate) {
                rec.setIsActive(false);
            }
            recurringRepo.save(rec);
        }
    }
}