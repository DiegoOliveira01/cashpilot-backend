package com.diego.cashpilot.controller;

import com.diego.cashpilot.dto.CreditCardRequestDTO;
import com.diego.cashpilot.dto.CreditCardResponseDTO;
import com.diego.cashpilot.service.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService service;

    @GetMapping
    public List<CreditCardResponseDTO> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCardResponseDTO create(@RequestBody CreditCardRequestDTO request) {
        return service.create(request);
    }
}