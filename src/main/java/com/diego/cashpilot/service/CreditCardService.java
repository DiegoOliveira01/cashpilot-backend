package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CreditCardRequestDTO;
import com.diego.cashpilot.dto.CreditCardResponseDTO;
import com.diego.cashpilot.model.CreditCard;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.repository.CreditCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository repository;

    public List<CreditCardResponseDTO> findAll() {
        User user = getLoggedUser();
        return repository.findByUserId(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public CreditCardResponseDTO create(CreditCardRequestDTO req) {
        User user = getLoggedUser();

        CreditCard card = new CreditCard();
        card.setUser(user);

        card.setName(req.getName());
        card.setBrand(req.getBrand());
        card.setClosingDay(req.getClosingDay());
        card.setDueDay(req.getDueDay());
        card.setLimitAmount(req.getLimitAmount());
        card.setImageUrl(req.getImageUrl());
        card.setIsActive(true);

        repository.save(card);
        return toResponse(card);
    }

    private CreditCardResponseDTO toResponse(CreditCard card) {
        return CreditCardResponseDTO.builder()
                .id(card.getId())
                .name(card.getName())
                .brand(card.getBrand())
                .closingDay(card.getClosingDay())
                .dueDay(card.getDueDay())
                .limitAmount(card.getLimitAmount())
                .imageUrl(card.getImageUrl())
                .isActive(card.getIsActive())
                .build();
    }

    private User getLoggedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}