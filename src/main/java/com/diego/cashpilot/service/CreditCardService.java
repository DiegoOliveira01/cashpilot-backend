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
        card.setName(req.name());
        card.setBrand(req.brand());
        card.setClosingDay(req.closingDay());
        card.setDueDay(req.dueDay());
        card.setLimitAmount(req.limitAmount());
        card.setImageUrl(req.imageUrl());
        card.setIsActive(true);

        repository.save(card);
        return toResponse(card);
    }

    private CreditCardResponseDTO toResponse(CreditCard card) {
        return new CreditCardResponseDTO(
                card.getId(), card.getName(), card.getBrand(),
                card.getClosingDay(), card.getDueDay(),
                card.getLimitAmount(), card.getImageUrl(), card.getIsActive()
        );
    }

    private User getLoggedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}