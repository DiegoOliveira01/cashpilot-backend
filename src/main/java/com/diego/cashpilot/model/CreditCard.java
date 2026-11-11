package com.diego.cashpilot.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "credit_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name; // Ex: "Nubank", "Cartão Principal"

    private String brand; // Ex: "Visa", "Mastercard"

    @Column(nullable = false)
    private Integer closingDay; // Dia de fechamento (ex: 25)

    @Column(nullable = false)
    private Integer dueDay; // Dia de vencimento (ex: 5)

    @Column(nullable = false)
    private BigDecimal limitAmount; // Limite total do cartão

    private String color; // Ex: "#8A05BE" (Roxo do Nubank)

    @Column(nullable = false) // Guarda a URL ou o caminho da imagem (ex: "/assets/cards/nubank-1280x720.jpg")
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isActive = true;
}