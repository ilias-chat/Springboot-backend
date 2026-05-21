package com.dwsc.backend.football;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiFootballServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizeApiKey_stripsAccidentalPrefix() {
        assertEquals("abc123", ApiFootballService.normalizeApiKey("API_FOOTBALL_KEY=abc123"));
        assertEquals("abc123", ApiFootballService.normalizeApiKey("  api_football_key=abc123  "));
        assertEquals("abc123", ApiFootballService.normalizeApiKey("abc123"));
    }

    @Test
    void formatApiFootballErrors_parsesObjectShape() throws Exception {
        var errors = objectMapper.readTree("{\"token\":\"Invalid API key\",\"requests\":\"Limit\"}");
        String msg = ApiFootballService.formatApiFootballErrors(errors);
        assertTrue(msg.contains("token"));
        assertTrue(msg.contains("Invalid API key"));
    }

    @Test
    void formatApiFootballErrors_parsesArrayShape() throws Exception {
        var errors = objectMapper.readTree("[{\"message\":\"Season not found\"}]");
        String msg = ApiFootballService.formatApiFootballErrors(errors);
        assertEquals("Season not found", msg);
    }
}
