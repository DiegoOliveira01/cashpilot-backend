package com.diego.cashpilot.controller;

import com.diego.cashpilot.dto.RecurringTransactionRequestDTO;
import com.diego.cashpilot.dto.RecurringTransactionResponseDTO;
import com.diego.cashpilot.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @GetMapping
    public List<RecurringTransactionResponseDTO> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTransactionResponseDTO  create(@RequestBody RecurringTransactionRequestDTO  request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/toggle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggle(@PathVariable Long id) {
        service.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}