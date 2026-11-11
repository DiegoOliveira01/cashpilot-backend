package com.diego.cashpilot.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "credit_card_invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreditCardInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    // Mês de referência (ex: "2026-11") para facilitar a busca e evitar duplicidade
    @Column(nullable = false)
    private String referenceMonth;

    @Column(nullable = false)
    private LocalDate closingDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    // Se a fatura foi gerada por um ajuste manual do usuário (não soma as compras individuais)
    @Column(nullable = false)
    private Boolean isManual = false;

    // Status
    @Column(nullable = false)
    private Boolean isClosed = false; // Fechada ou aberta para novas compras
    @Column(nullable = false)
    private Boolean isPaid = false; // Paga ou pendente
}