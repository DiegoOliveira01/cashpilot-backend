package com.diego.cashpilot.repository;

import com.diego.cashpilot.model.TelegramLink;
import com.diego.cashpilot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramLinkRepository extends JpaRepository<TelegramLink, Long> {

    Optional<TelegramLink> findByChatId(Long chatId);

    Optional<TelegramLink> findByUser(User user);
}
