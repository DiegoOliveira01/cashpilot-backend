package com.diego.cashpilot.model;

import com.diego.cashpilot.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Emoji opcional, usado no dashboard e no bot.
    private String icon;

    // null = categoria padrão do sistema (visível para todos); preenchido = categoria do usuário.
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
