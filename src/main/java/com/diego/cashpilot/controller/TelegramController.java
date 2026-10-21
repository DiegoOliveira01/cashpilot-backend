package com.diego.cashpilot.controller;

import com.diego.cashpilot.dto.TelegramUpdate;
import com.diego.cashpilot.model.User;
import com.diego.cashpilot.service.TelegramBotService;
import com.diego.cashpilot.service.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramBotService botService;
    private final TelegramLinkService linkService;

    @Value("${telegram.webhook-secret}")
    private String webhookSecret;

    /** Autenticado por JWT: o dashboard chama para gerar o código de vínculo. */
    @PostMapping("/link-code")
    public TelegramLinkService.LinkCode generateLinkCode(@AuthenticationPrincipal User user) {
        return linkService.generateCode(user);
    }

    /** Público no Spring Security, protegido pelo secret token que o Telegram envia no header. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody TelegramUpdate update) {

        if (secret == null || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(403).build();
        }

        try {
            botService.handle(update);
        } catch (Exception e) {
            // Sempre 200: se devolver erro, o Telegram reenvia o mesmo update em loop.
            log.error("Erro ao processar update do Telegram: {}", e.getClass().getSimpleName());
        }
        return ResponseEntity.ok().build();
    }
}
