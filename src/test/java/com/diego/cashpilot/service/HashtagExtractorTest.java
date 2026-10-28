package com.diego.cashpilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HashtagExtractorTest {

    @Test
    void extractsTagAndCleansTheText() {
        var result = HashtagExtractor.extract("/gasto 35,90 jantar #lazer");

        assertEquals("/gasto 35,90 jantar", result.text());
        assertEquals("lazer", result.tag());
    }

    @Test
    void tagCanBeInTheMiddleAndHaveAccents() {
        var result = HashtagExtractor.extract("gastei #saúde 40 na farmácia");

        assertEquals("gastei 40 na farmácia", result.text());
        assertEquals("saúde", result.tag());
    }

    @Test
    void usesTheFirstTagAndRemovesAll() {
        var result = HashtagExtractor.extract("gastei 40 #lazer #outros");

        assertEquals("gastei 40", result.text());
        assertEquals("lazer", result.tag());
    }

    @Test
    void numbersAfterHashAreNotTags() {
        var result = HashtagExtractor.extract("paguei 50 do pedido #123");

        assertEquals("paguei 50 do pedido #123", result.text());
        assertNull(result.tag());
    }

    @Test
    void messageWithoutTagIsUntouched() {
        var result = HashtagExtractor.extract("gastei 40 no mercado");

        assertEquals("gastei 40 no mercado", result.text());
        assertNull(result.tag());
    }
}
