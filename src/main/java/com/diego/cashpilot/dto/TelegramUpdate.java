package com.diego.cashpilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Subconjunto do objeto Update do Telegram: só o que o bot usa. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
        @JsonProperty("update_id") long updateId,
        Message message
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(Chat chat, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(long id, String type) {}
}
