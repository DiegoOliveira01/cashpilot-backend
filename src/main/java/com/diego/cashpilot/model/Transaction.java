package com.diego.cashpilot.model;

import com.diego.cashpilot.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String description;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private LocalDate date;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPaid = true; // Manual = true por padrão, Recorrente = false

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // transações antigas (e as do bot, por enquanto) ficam sem categoria.
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "recurring_transaction_id")
    private RecurringTransaction recurringTransaction;

    // A data exata que esta ocorrência representa (pode ser diferente da data de criação)
    @Column(name = "recurrence_date")
    private LocalDate recurrenceDate;

    @ManyToOne
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @ManyToOne
    @JoinColumn(name = "credit_card_invoice_id")
    private CreditCardInvoice creditCardInvoice;

    // Identificador único para agrupar as parcelas da mesma compra (ex: a compra do celular em 10x)
    @Column(name = "purchase_group_id")
    private String purchaseGroupId;

    // Qual é o número desta parcela (ex: 1, 2, 3...)
    private Integer installmentNumber;

    // Quantas parcelas no total (ex: 10)
    private Integer totalInstallments;

}
