package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.RecurringTransactionRequestDTO;
import com.diego.cashpilot.dto.RecurringTransactionResponseDTO;
import com.diego.cashpilot.model.Category;
import com.diego.cashpilot.model.RecurringTransaction;
import com.diego.cashpilot.repository.CategoryRepository;
import com.diego.cashpilot.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final CategoryRepository categoryRepository;

    public List<RecurringTransactionResponseDTO> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public RecurringTransactionResponseDTO  create(RecurringTransactionRequestDTO  req) {
        RecurringTransaction rec = new RecurringTransaction();
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

    public void delete(Long id) {
        // Apenas deleta o molde. As transações geradas continuam no histórico.
        repository.deleteById(id);
    }

    private RecurringTransactionResponseDTO  toResponse(RecurringTransaction rec) {
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