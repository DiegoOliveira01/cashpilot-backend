package com.diego.cashpilot.service;

import com.diego.cashpilot.dto.CategoryResponseDTO;
import com.diego.cashpilot.dto.CategorySummaryDTO;
import com.diego.cashpilot.dto.TransactionRequestDTO;
import com.diego.cashpilot.dto.TransactionResponseDTO;
import com.diego.cashpilot.dto.TransactionSummaryDTO;
import com.diego.cashpilot.model.*;
import com.diego.cashpilot.model.enums.TransactionType;
import com.diego.cashpilot.repository.CategoryRepository;
import com.diego.cashpilot.repository.CreditCardRepository;
import com.diego.cashpilot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    private final CreditCardRepository creditCardRepository;
    private final CreditCardCalculator creditCardCalculator;
    private final CreditCardInvoiceService creditCardInvoiceService;

    public TransactionResponseDTO create(TransactionRequestDTO dto){
        return createForUser(getLoggedUser(), dto);
    }

    // Usado pelo bot do Telegram: o webhook não tem JWT, então o usuário vem explícito.
    public TransactionResponseDTO createForUser(User user, TransactionRequestDTO dto){

        // LÓGICA PARA COMPRAS NO CARTÃO DE CRÉDITO (PARCELADAS)
        if (dto.getCreditCardId() != null) {
            CreditCard card = creditCardRepository.findById(dto.getCreditCardId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cartão não encontrado"));

            // Se não vier a quantidade de parcelas, assume 1x
            int installments = dto.getInstallments() != null && dto.getInstallments() > 0 ? dto.getInstallments() : 1;

            // Calcula os valores e as datas de vencimento de cada parcela
            List<CreditCardCalculator.InstallmentInfo> infos = creditCardCalculator.calculateInstallments(
                    card, dto.getDate(), dto.getAmount(), installments
            );

            String groupId = UUID.randomUUID().toString(); // Agrupa as parcelas
            Transaction firstTransaction = null;

            for (CreditCardCalculator.InstallmentInfo info : infos) {
                // Busca ou cria a fatura para a data dessa parcela
                CreditCardInvoice invoice = creditCardInvoiceService.getOrCreateInvoice(card, info.date());

                Transaction t = Transaction.builder()
                        .description(dto.getDescription() + " (" + info.number() + "/" + info.total() + ")")
                        .amount(info.amount())
                        .type(dto.getType())
                        .date(info.date())
                        .category(resolveCategory(user, dto.getCategoryId(), dto.getType()))
                        .user(user)
                        .creditCard(card)
                        .creditCardInvoice(invoice) // Vincula à fatura
                        .purchaseGroupId(groupId)
                        .installmentNumber(info.number())
                        .totalInstallments(info.total())
                        .isPaid(false)
                        .build();

                transactionRepository.save(t);

                // Atualiza o valor total da fatura
                creditCardInvoiceService.addTransactionToInvoice(invoice, info.amount());

                if (firstTransaction == null) firstTransaction = t;
            }

            return mapToResponse(firstTransaction);
        }

        // LÓGICA PARA TRANSAÇÕES NORMAIS
        Transaction transaction = Transaction.builder()
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .type(dto.getType())
                .date(dto.getDate())
                .category(resolveCategory(user, dto.getCategoryId(), dto.getType()))
                .user(user)
                .isPaid(true) // Transação manual nasce paga
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
        transaction.setCategory(resolveCategory(user, dto.getCategoryId(), dto.getType()));

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
                .isPaid(transaction.getIsPaid())
                .category(CategoryResponseDTO.of(transaction.getCategory()))
                .creditCardId(transaction.getCreditCard() != null ? transaction.getCreditCard().getId() : null)
                .purchaseGroupId(transaction.getPurchaseGroupId())
                .installmentNumber(transaction.getInstallmentNumber())
                .totalInstallments(transaction.getTotalInstallments())
                .build();
    }

    /**
     * Sem categoryId = sem categoria. Com categoryId, ela precisa ser padrão ou do usuário
     * e ter o mesmo tipo da transação (não dá para classificar uma receita como "Alimentação").
     */
    private Category resolveCategory(User user, Long categoryId, TransactionType type){
        if (categoryId == null) return null;

        Category category = categoryRepository.findAvailableById(categoryId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inválida"));

        if (category.getType() != type) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A categoria não é compatível com o tipo da transação");
        }
        return category;
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

    public List<CategorySummaryDTO> getSummaryByCategory(YearMonth month){
        return getSummaryByCategoryForUser(getLoggedUser(), month);
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> getSummaryByCategoryForUser(User user, YearMonth month){
        List<Transaction> inMonth = transactionRepository
                .findByUserAndDateBetween(user, month.atDay(1), month.atEndOfMonth());

        return CategorySummarizer.summarize(inMonth);
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

    // Último lançamento CRIADO (maior id): é o alvo de /desfazer e de /categoria no bot.
    @Transactional(readOnly = true)
    public Optional<TransactionResponseDTO> findLastCreatedForUser(User user){
        return transactionRepository.findFirstByUserOrderByIdDesc(user).map(this::mapToResponse);
    }

    // Troca a categoria do último lançamento criado. category == null remove a categoria.
    @Transactional
    public Optional<TransactionResponseDTO> recategorizeLatestForUser(User user, Category category){
        return transactionRepository.findFirstByUserOrderByIdDesc(user).map(transaction -> {
            if (category != null && category.getType() != transaction.getType()) {
                throw new IllegalArgumentException("Essa categoria não combina com o tipo do último lançamento.");
            }
            transaction.setCategory(category);
            return mapToResponse(transactionRepository.save(transaction));
        });
    }

    public void markAsPaid(Long id) {
        User user = getLoggedUser();
        Transaction t = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        t.setIsPaid(true);
        transactionRepository.save(t);
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