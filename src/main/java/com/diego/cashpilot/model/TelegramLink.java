package com.diego.cashpilot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    // Consentimento para enviar frases ao Gemini. Nullable de propósito: o Hibernate
    // adiciona a coluna sem quebrar as linhas que já existem (null = não consentiu).
    @Column(name = "ai_consent")
    private Boolean aiConsent;
}
