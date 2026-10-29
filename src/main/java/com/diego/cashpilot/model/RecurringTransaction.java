package com.diego.cashpilot.model;

import com.diego.cashpilot.model.enums.RecurrenceFrequency;
import com.diego.cashpilot.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceFrequency frequency;

    // Para recorrências TWICE_MONTHLY (ex: dias 5 e 20)
    private Integer firstDayOfMonth;
    private Integer secondDayOfMonth;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate; // Opcional
    private Integer maxOccurrences; // Opcional

    @Column(nullable = false)
    private Integer occurrencesGenerated = 0;

    // Ponteiro para a próxima data a ser gerada
    @Column(nullable = false)
    private LocalDate nextOccurrenceDate;

    @Column(nullable = false)
    private Boolean isActive = true;
}