package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.dto.TransactionResponseDTO;
import com.diego.cashpilot.dto.TransactionSummaryDTO;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import com.diego.cashpilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        return createForUser(getLoggedUser(), dto);
    }

    public TransactionResponseDTO createForUser(User user, TransactionRequestDTO dto) {
        Transaction transaction = Transaction.builder()
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .type(dto.getType())
                .date(dto.getDate())
                .user(user)
                .build();

        return mapToResponse(transactionRepository.save(transaction));
    }

    public TransactionResponseDTO update(Long id, TransactionRequestDTO dto){
        User user = getLoggedUser();

        Transaction transaction = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setDate(dto.getDate());

        transaction = transactionRepository.save(transaction);

        return mapToResponse(transaction);
    }


    public List<TransactionResponseDTO> findAllByUser(){
        User user = getLoggedUser();

        return transactionRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TransactionResponseDTO findById(Long id){
        User user = getLoggedUser();

        Transaction transaction = transactionRepository
                .findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        return mapToResponse(transaction);
    }

    private TransactionResponseDTO mapToResponse(Transaction transaction){
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .date(transaction.getDate())
                .build();
    }

    public TransactionSummaryDTO getSummary(){
        User user = getLoggedUser();

        List<Transaction> transactions = transactionRepository.findByUser(user);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Transaction t : transactions){
            if (t.getType() == TransactionType.INCOME){
                income = income.add(t.getAmount());
            }
            else {
                expense = expense.add(t.getAmount());
            }
        }

        BigDecimal balance = income.subtract(expense);

        return TransactionSummaryDTO.builder()
                .income(income)
                .expense(expense)
                .balance(balance)
                .build();

    }

    public void delete(Long id){
        User user = getLoggedUser();

        Transaction transaction = transactionRepository
                .findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));

        transactionRepository.delete(transaction);
    }

    private User getLoggedUser() {
        return (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

}
