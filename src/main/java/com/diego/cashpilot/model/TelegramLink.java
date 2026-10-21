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
}
