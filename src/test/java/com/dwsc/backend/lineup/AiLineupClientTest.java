package com.dwsc.backend.lineup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiLineupClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiLineupClient client() {
        return new AiLineupClient(
                "gsk_test",
                "",
                "grok-3-mini",
                "llama-3.3-70b-versatile",
                "https://api.x.ai/v1",
                "https://api.groq.com/openai/v1",
                objectMapper);
    }

    @Test
    void parseJsonFromText_parsesPlainJson() throws Exception {
        JsonNode node = client().parseJsonFromText("{\"formation\":\"4-4-2\"}");
        assertEquals("4-4-2", node.path("formation").asText());
    }

    @Test
    void parseJsonFromText_stripsMarkdownFences() throws Exception {
        JsonNode node = client().parseJsonFromText("```json\n{\"formation\":\"4-3-3\"}\n```");
        assertEquals("4-3-3", node.path("formation").asText());
    }

    @Test
    void parseJsonFromText_extractsEmbeddedObject() throws Exception {
        JsonNode node = client().parseJsonFromText("Here is your team: {\"formation\":\"3-5-2\"} enjoy");
        assertEquals("3-5-2", node.path("formation").asText());
    }

    @Test
    void parseJsonFromText_throwsWhenNoJson() {
        assertThrows(Exception.class, () -> client().parseJsonFromText("no json here"));
    }
}
