package com.diego.cashpilot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TelegramClient {

    private final RestClient restClient;

    public TelegramClient(RestClient.Builder builder,
                          @Value("${telegram.bot-token}") String token) {
        // O token faz parte da URL da API: nunca logue a URL completa.
        this.restClient = builder
                .baseUrl("https://api.telegram.org/bot" + token)
                .build();
    }

    public void sendMessage(long chatId, String text) {
        restClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
    }
}
