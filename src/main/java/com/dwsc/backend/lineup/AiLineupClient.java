package com.dwsc.backend.lineup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI lineup client — supports xAI Grok ({@code xai-} keys) and Groq ({@code gsk_} keys).
 * Port of TRWM-backend {@code services/grokService.js}; provider is auto-detected from the key prefix.
 */
@Component
public class AiLineupClient {

    private static final String XAI_DEFAULT_BASE = "https://api.x.ai/v1";
    private static final String XAI_DEFAULT_MODEL = "grok-3-mini";
    private static final String GROQ_DEFAULT_BASE = "https://api.groq.com/openai/v1";
    private static final String GROQ_DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final Pattern FENCED_JSON = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final String apiKey;
    private final String xaiModel;
    private final String groqModel;
    private final String xaiBase;
    private final String groqBase;
    private final ObjectMapper objectMapper;

    public AiLineupClient(
            @Value("${GROK_API_KEY:}") String grokKey,
            @Value("${GROQ_API_KEY:}") String groqKey,
            @Value("${GROK_MODEL:" + XAI_DEFAULT_MODEL + "}") String xaiModel,
            @Value("${GROQ_MODEL:" + GROQ_DEFAULT_MODEL + "}") String groqModel,
            @Value("${GROK_API_BASE:" + XAI_DEFAULT_BASE + "}") String xaiBase,
            @Value("${GROQ_API_BASE:" + GROQ_DEFAULT_BASE + "}") String groqBase,
            ObjectMapper objectMapper) {
        String key = grokKey != null && !grokKey.isBlank() ? grokKey : groqKey;
        this.apiKey = key != null ? key.trim() : "";
        this.xaiModel = xaiModel;
        this.groqModel = groqModel;
        this.xaiBase = xaiBase.replaceAll("/+$", "");
        this.groqBase = groqBase.replaceAll("/+$", "");
        this.objectMapper = objectMapper;
    }

    /** Roster row sent to the model. */
    public record LineupPlayerInput(
            String id,
            String name,
            String team,
            String position,
            Double avgRating,
            long reviewCount,
            String statsSummary) {}

    public record SlotPick(String playerId, String slot, String role) {}

    public record LineupSuggestion(
            String formation, String reasoning, List<SlotPick> starters, List<SlotPick> bench) {}

    private record Provider(String name, String baseUrl, String model) {}

    private Provider resolveProvider() {
        if (apiKey.startsWith("gsk_")) {
            return new Provider("Groq", groqBase, groqModel);
        }
        return new Provider("xAI Grok", xaiBase, xaiModel);
    }

    private String requireApiKey() {
        if (apiKey.isBlank() || "your_grok_api_key_here".equals(apiKey) || "your_api_key_here".equals(apiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GROK_API_KEY is not configured. Add a Groq key (gsk_… from console.groq.com) or xAI Grok key "
                            + "(xai-… from console.x.ai) to the environment and restart the service.");
        }
        return apiKey;
    }

    public LineupSuggestion suggestLineup(List<LineupPlayerInput> players, String formation) {
        String key = requireApiKey();
        Provider provider = resolveProvider();
        String prompt = buildPrompt(players, formation);
        String url = provider.baseUrl() + "/chat/completions";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", provider.model());
        body.put("temperature", 0.4);
        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put(
                "content",
                "You are a football manager assistant. Respond with valid JSON only — no markdown fences or extra text.");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);

        String responseBody;
        try {
            responseBody =
                    RestClient.create()
                            .post()
                            .uri(url)
                            .header("Authorization", "Bearer " + key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body.toString())
                            .retrieve()
                            .body(String.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(mapErrorStatus(e.getStatusCode().value()), extractError(provider, e));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, provider.name() + " request failed: " + e.getMessage());
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, provider.name() + " returned an unreadable response.");
        }

        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, provider.name() + " returned an empty lineup response.");
        }

        JsonNode parsed;
        try {
            parsed = parseJsonFromText(content.asText());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, provider.name() + " returned invalid JSON for the lineup.");
        }

        return normalize(parsed, formation);
    }

    private static HttpStatus mapErrorStatus(int status) {
        if (status >= 400 && status < 500) {
            HttpStatus resolved = HttpStatus.resolve(status);
            return resolved != null ? resolved : HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String extractError(Provider provider, RestClientResponseException e) {
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            JsonNode error = body.path("error");
            if (error.isObject() && error.path("message").isTextual()) {
                return error.path("message").asText();
            }
            if (error.isTextual() && !error.asText().isBlank()) {
                return error.asText();
            }
        } catch (Exception ignored) {
            // fall through to generic message
        }
        return provider.name() + " API returned HTTP " + e.getStatusCode().value();
    }

    JsonNode parseJsonFromText(String text) throws Exception {
        String trimmed = text.trim();
        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
            Matcher fenced = FENCED_JSON.matcher(trimmed);
            if (fenced.find()) {
                return objectMapper.readTree(fenced.group(1).trim());
            }
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readTree(trimmed.substring(start, end + 1));
            }
            throw new IllegalArgumentException("No JSON object found");
        }
    }

    private LineupSuggestion normalize(JsonNode raw, String defaultFormation) {
        if (raw == null || !raw.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI lineup JSON was not an object.");
        }
        String formation =
                raw.path("formation").isTextual() && !raw.path("formation").asText().isBlank()
                        ? raw.path("formation").asText().trim()
                        : defaultFormation;
        String reasoning = raw.path("reasoning").isTextual() ? raw.path("reasoning").asText().trim() : "";
        List<SlotPick> starters = normalizePicks(raw.path("starters"), "starters");
        List<SlotPick> bench = normalizePicks(raw.path("bench"), "bench");
        return new LineupSuggestion(formation, reasoning, starters, bench);
    }

    private List<SlotPick> normalizePicks(JsonNode value, String label) {
        List<SlotPick> picks = new ArrayList<>();
        if (value == null || value.isMissingNode() || value.isNull()) {
            return picks;
        }
        if (!value.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI lineup " + label + " must be an array.");
        }
        int index = 0;
        for (JsonNode item : value) {
            if (!item.isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI lineup " + label + "[" + index + "] is invalid.");
            }
            String playerId = item.path("playerId").asText("").trim();
            if (playerId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI lineup " + label + "[" + index + "] is missing playerId.");
            }
            String slot = item.path("slot").asText("").trim();
            String role = item.path("role").isTextual() ? item.path("role").asText().trim() : null;
            picks.add(new SlotPick(playerId, slot, role));
            index++;
        }
        return picks;
    }

    private String buildPrompt(List<LineupPlayerInput> players, String formation) {
        ArrayNode roster = objectMapper.createArrayNode();
        for (LineupPlayerInput p : players) {
            ObjectNode row = roster.addObject();
            row.put("id", p.id());
            row.put("name", p.name());
            row.put("team", p.team());
            row.put("position", p.position() != null ? p.position() : "Unknown");
            if (p.avgRating() != null) {
                row.put("avgRating", p.avgRating());
            } else {
                row.putNull("avgRating");
            }
            row.put("reviewCount", p.reviewCount());
            if (p.statsSummary() != null) {
                row.put("statsSummary", p.statsSummary());
            } else {
                row.putNull("statsSummary");
            }
        }

        return String.join(
                "\n",
                "Pick the best starting XI from the full local player database roster below.",
                "Use formation " + formation
                        + " unless the roster cannot support it — then pick the closest valid formation and explain why.",
                "Rules:",
                "- Pick the strongest possible team from the entire roster (all teams and leagues).",
                "- Pick exactly 11 starters with distinct player ids from the roster.",
                "- Include exactly one goalkeeper in starters when possible.",
                "- Prefer players with higher avgRating, stronger stats, and positions that fit the formation.",
                "- bench may include up to 7 substitutes from remaining roster players (optional).",
                "- Every playerId MUST be copied exactly from the roster id field.",
                "",
                "Respond with JSON only, no markdown, using this shape:",
                "{\"formation\":\"4-4-2\",\"reasoning\":\"...\",\"starters\":[{\"playerId\":\"...\",\"slot\":\"GK\","
                        + "\"role\":\"Goalkeeper\"}],\"bench\":[{\"playerId\":\"...\",\"slot\":\"SUB1\",\"role\":\"Midfielder\"}]}",
                "",
                "Roster:",
                roster.toPrettyString());
    }
}
