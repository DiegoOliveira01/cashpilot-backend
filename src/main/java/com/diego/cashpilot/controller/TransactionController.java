package com.diego.cashpilot.controller;

import com.diego.cashpilot.dto.CategorySummaryDTO;
import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.dto.TransactionResponseDTO;
import com.diego.cashpilot.dto.TransactionSummaryDTO;
import com.diego.cashpilot.service.RecurrenceProcessorService;
import com.diego.cashpilot.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final TransactionService transactionService;
    private final RecurrenceProcessorService recurrenceProcessor;

    @PostMapping
    public TransactionResponseDTO create(@RequestBody @Valid TransactionRequestDTO dto){
        return transactionService.create(dto);
    }

    @PutMapping("/{id}")
    public TransactionResponseDTO update(@PathVariable Long id, @RequestBody @Valid TransactionRequestDTO dto){
        return transactionService.update(id, dto);
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO getById(@PathVariable Long id){
        return transactionService.findById(id);
    }

    @GetMapping
    public List<TransactionResponseDTO> getAll(){
        return transactionService.findAllByUser();
    }

    @GetMapping("/summary")
    public TransactionSummaryDTO getSummary(){
        recurrenceProcessor.processDueRecurrences(); // Antes de retornar as transações, gera as pendentes

        return transactionService.getSummary();
    }

    // Totais por categoria no mês (ex.: ?month=2026-10). Sem o parâmetro, usa o mês atual.
    @GetMapping("/summary/by-category")
    public List<CategorySummaryDTO> getSummaryByCategory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month){
        return transactionService.getSummaryByCategory(month != null ? month : YearMonth.now(ZONE));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        transactionService.delete(id);
    }
}
