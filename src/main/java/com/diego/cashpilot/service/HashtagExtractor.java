package com.diego.cashpilot.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Separa a "#categoria" do resto da mensagem ("/gasto 35,90 jantar #lazer"). */
public final class HashtagExtractor {

    public record Result(String text, String tag) {}

    // "#" no início ou depois de espaço, com pelo menos uma letra ("#123" de um pedido não conta).
    private static final Pattern HASHTAG =
            Pattern.compile("(?<!\\S)#(?=[\\p{L}\\p{N}_-]*\\p{L})([\\p{L}\\p{N}_-]+)");

    private HashtagExtractor() {}

    /** Devolve a mensagem sem as hashtags e a primeira delas (ou null). */
    public static Result extract(String text) {
        if (text == null) return new Result("", null);

        String tag = null;
        Matcher matcher = HASHTAG.matcher(text);
        while (matcher.find()) {
            if (tag == null) tag = matcher.group(1);
        }

        String clean = HASHTAG.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim();
        return new Result(clean, tag);
    }
}
