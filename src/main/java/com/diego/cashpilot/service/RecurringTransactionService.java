package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.RecurringTransactionRequestDTO;
import com.diego.cashpilot.dto.RecurringTransactionResponseDTO;
import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.RecurringTransaction;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.repository.CategoryRepository;
import com.diego.cashpilot.repository.RecurringTransactionRepository;
import com.diego.cashpilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public List<RecurringTransactionResponseDTO> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public RecurringTransactionResponseDTO create(RecurringTransactionRequestDTO  req) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        RecurringTransaction rec = new RecurringTransaction();
        rec.setUser(user);
        rec.setDescription(req.description());
        rec.setAmount(req.amount());
        rec.setType(req.type());
        rec.setFrequency(req.frequency());
        rec.setFirstDayOfMonth(req.firstDayOfMonth());
        rec.setSecondDayOfMonth(req.secondDayOfMonth());
        rec.setStartDate(req.startDate());
        rec.setEndDate(req.endDate());
        rec.setMaxOccurrences(req.maxOccurrences());

        // A primeira ocorrência será gerada na data de início
        rec.setNextOccurrenceDate(req.startDate());
        rec.setOccurrencesGenerated(0);
        rec.setIsActive(true);

        if (req.categoryId() != null) {
            Category cat = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            rec.setCategory(cat);
        }

        repository.save(rec);
        return toResponse(rec);
    }

    public void toggleActive(Long id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recorrência não encontrada"));
        rec.setIsActive(!rec.getIsActive());
        repository.save(rec);
    }

    @Transactional
    public void delete(Long id) {
        // 1. Desvincula as transações históricas (elas ficam salvas, mas sem o vínculo com a recorrência)
        transactionRepository.detachRecurringTransactions(id);

        // 2. Deleta a recorrência
        repository.deleteById(id);
    }

    private RecurringTransactionResponseDTO toResponse(RecurringTransaction rec) {
        return new RecurringTransactionResponseDTO(
                rec.getId(),
                rec.getDescription(),
                rec.getAmount(),
                rec.getType(),
                rec.getCategory() != null ? rec.getCategory().getId() : null,
                rec.getCategory() != null ? rec.getCategory().getName() : null,
                rec.getCategory() != null ? rec.getCategory().getIcon() : null,
                rec.getFrequency(),
                rec.getStartDate(),
                rec.getNextOccurrenceDate(),
                rec.getIsActive()
        );
    }
}