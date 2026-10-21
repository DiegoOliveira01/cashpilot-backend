package com.diego.cashpilot.service;

import com.diego.cashpilot.model.TelegramLink;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.repository.TelegramLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TelegramLinkService {

    public record LinkCode(String code, String deepLink, int expiresInMinutes) {}

    private record PendingLink(User user, Instant expiresAt) {}

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    // Em memória: simples e suficiente com 1 instância. Se reiniciar, o usuário só gera outro código.
    private final Map<String, PendingLink> pending = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final TelegramLinkRepository repository;

    @Value("${telegram.bot-username}")
    private String botUsername;

    public LinkCode generateCode(User user) {
        Instant now = Instant.now();
        pending.values().removeIf(p -> p.expiresAt().isBefore(now)
                || p.user().getId().equals(user.getId()));

        String code = randomCode(10);
        pending.put(code, new PendingLink(user, now.plus(TTL)));

        return new LinkCode(code,
                "https://t.me/" + botUsername + "?start=" + code,
                (int) TTL.toMinutes());
    }

    /** Consome o código (uso único) e vincula o chat ao usuário. */
    @Transactional
    public Optional<User> consumeCode(String code, long chatId) {
        PendingLink link = pending.remove(code);
        if (link == null || link.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        // Um chat por usuário e um usuário por chat: substitui vínculos antigos.
        repository.findByChatId(chatId).ifPresent(repository::delete);
        repository.findByUser(link.user()).ifPresent(repository::delete);
        repository.flush();

        repository.save(TelegramLink.builder()
                .user(link.user())
                .chatId(chatId)
                .linkedAt(LocalDateTime.now())
                .build());

        return Optional.of(link.user());
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByChatId(long chatId) {
        return repository.findByChatId(chatId).map(TelegramLink::getUser);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
