package com.dwsc.backend.football;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiFootballServiceTest {

    @Test
    void normalizeApiKey_stripsAccidentalPrefix() {
        assertEquals("abc123", ApiFootballService.normalizeApiKey("API_FOOTBALL_KEY=abc123"));
        assertEquals("abc123", ApiFootballService.normalizeApiKey("  api_football_key=abc123  "));
        assertEquals("abc123", ApiFootballService.normalizeApiKey("abc123"));
    }
}
