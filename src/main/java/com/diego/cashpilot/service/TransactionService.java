package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.dto.TransactionResponseDTO;
import com.diego.cashpilot.dto.TransactionSummaryDTO;
import com.diego.cashpilot.model.Transaction;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.model.enums.TransactionType;
import com.diego.cashpilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionResponseDTO create(TransactionRequestDTO dto){
        return createForUser(getLoggedUser(), dto);
    }

    // Usado pelo bot do Telegram: o webhook não tem JWT, então o usuário vem explícito.
    public TransactionResponseDTO createForUser(User user, TransactionRequestDTO dto){

        Transaction transaction = Transaction.builder()
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .type(dto.getType())
                .date(dto.getDate())
                .user(user)
                .build();

        transaction = transactionRepository.save(transaction);

        return mapToResponse(transaction);
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
        return summarize(transactionRepository.findByUser(getLoggedUser()));
    }

    // Resumo de um mês específico, no mesmo critério do dashboard (que filtra por mês).
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getSummaryForUser(User user, YearMonth month){
        List<Transaction> inMonth = transactionRepository.findByUser(user)
                .stream()
                .filter(t -> t.getDate() != null && YearMonth.from(t.getDate()).equals(month))
                .toList();

        return summarize(inMonth);
    }

    private TransactionSummaryDTO summarize(List<Transaction> transactions){
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

    // Mais recentes primeiro (por data e, no empate, pelo último criado).
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> findLatestForUser(User user, int limit){
        return transactionRepository
                .findByUserOrderByDateDescIdDesc(user, PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Remove o último lançamento CRIADO (maior id) e devolve o que foi removido.
    @Transactional
    public Optional<TransactionResponseDTO> deleteLatestForUser(User user){
        return transactionRepository.findFirstByUserOrderByIdDesc(user)
                .map(transaction -> {
                    transactionRepository.delete(transaction);
                    return mapToResponse(transaction);
                });
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
