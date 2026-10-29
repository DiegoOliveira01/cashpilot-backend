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

}
