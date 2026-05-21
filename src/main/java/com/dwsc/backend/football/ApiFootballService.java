package com.dwsc.backend.football;

import com.dwsc.backend.api.dto.FootballLeagueOption;
import com.dwsc.backend.api.dto.FootballTeamOption;
import com.dwsc.backend.model.GeoJsonPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API-Football (api-sports) v3 client — port of TRWM-backend {@code services/apiFootballService.js}.
 */
@Service
public class ApiFootballService {

    private static final String DEFAULT_BASE = "https://v3.football.api-sports.io";
    private static final List<Integer> TOP_LEAGUE_IDS =
            List.of(39, 140, 135, 78, 61, 2, 3, 88, 94, 40);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final String apiKey;
    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry<ImportPayloadResult>> importCache = new ConcurrentHashMap<>();

    public ApiFootballService(
            @Value("${API_FOOTBALL_KEY:}") String apiKey,
            @Value("${api-football.base-url:" + DEFAULT_BASE + "}") String baseUrl,
            ObjectMapper objectMapper) {
        this.apiKey = normalizeApiKey(apiKey);
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.objectMapper = objectMapper;
        this.restClient =
                RestClient.builder()
                        .baseUrl(this.baseUrl)
                        .defaultHeader("x-apisports-key", this.apiKey)
                        .build();
    }

    public void requireConfigured() {
        if (apiKey.isEmpty()) {
            throw new ApiFootballException("API_FOOTBALL_KEY is not set", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    public record TeamStadiumContext(String teamName, String leagueName, String venueName, GeoJsonPoint location) {}

    public record ImportPlayerDoc(
            String name,
            String team,
            String league,
            String image,
            Integer externalId,
            String position,
            JsonNode stats,
            String venueName,
            GeoJsonPoint location) {}

    public record ImportPayloadResult(List<ImportPlayerDoc> players, String teamName, String leagueName, String venueName) {}

    public TeamStadiumContext resolveTeamStadiumContext(int leagueId, int teamId, int season) {
        return resolveTeamStadiumContext(leagueId, teamId, season, false);
    }

    /**
     * Resolves team/league/venue names and optional stadium coordinates.
     *
     * @param allowMissingStadiumCoords when true (client sends device {@code lat}/{@code lng}),
     *     missing venue/geocode coords return {@code location=null} instead of 422.
     */
    public TeamStadiumContext resolveTeamStadiumContext(
            int leagueId, int teamId, int season, boolean allowMissingStadiumCoords) {
        requireConfigured();
        String leagueName = assertLeagueBelongsToTeam(teamId, leagueId, season);
        TeamRow teamRow = fetchTeam(teamId);
        String venueLabel = teamRow.venueName();
        Coords coords = teamRow.embeddedCoords();
        if (coords == null && teamRow.venueId() != null) {
            if (allowMissingStadiumCoords) {
                try {
                    VenuePoint pt = fetchVenuePoint(teamRow.venueId());
                    coords = new Coords(pt.lat(), pt.lng());
                    if (pt.venueName() != null && !pt.venueName().isBlank()) {
                        venueLabel = pt.venueName();
                    }
                } catch (ApiFootballException ignored) {
                    coords = null;
                }
            } else {
                try {
                    VenuePoint pt = fetchVenuePoint(teamRow.venueId());
                    coords = new Coords(pt.lat(), pt.lng());
                    if (pt.venueName() != null && !pt.venueName().isBlank()) {
                        venueLabel = pt.venueName();
                    }
                } catch (ApiFootballException e) {
                    if (e.getStatusCodeValue() == 422
                            && e.getReason() != null
                            && e.getReason().toLowerCase().contains("coordinate")) {
                        coords = geocodeNominatim(teamRow.city(), teamRow.country(), teamRow.venueName());
                    } else {
                        throw e;
                    }
                }
            }
        }
        GeoJsonPoint location = coords != null ? new GeoJsonPoint("Point", List.of(coords.lng(), coords.lat())) : null;
        return new TeamStadiumContext(teamRow.teamName(), leagueName, venueLabel, location);
    }

    public ImportPayloadResult buildImportPayloads(int leagueId, int teamId, int season) {
        requireConfigured();
        String cacheKey = leagueId + ":" + teamId + ":" + season;
        CacheEntry<ImportPayloadResult> cached = importCache.get(cacheKey);
        if (cached != null && Instant.now().toEpochMilli() - cached.at() < CACHE_TTL_MS) {
            return cached.data();
        }
        TeamStadiumContext ctx = resolveTeamStadiumContext(leagueId, teamId, season);
        if (ctx.location() == null) {
            throw new ApiFootballException(
                    "Could not resolve stadium coordinates (no coords on team venue, and no venue id for /venues or geocode)",
                    422);
        }
        List<JsonNode> rawPlayers = fetchAllPlayersPages(teamId, season);
        List<ImportPlayerDoc> players = new ArrayList<>();
        for (JsonNode row : rawPlayers) {
            ImportPlayerDoc doc = mapImportRow(row, ctx.teamName(), ctx.leagueName(), ctx.venueName(), ctx.location());
            if (doc != null) {
                players.add(doc);
            }
        }
        ImportPayloadResult result = new ImportPayloadResult(players, ctx.teamName(), ctx.leagueName(), ctx.venueName());
        importCache.put(cacheKey, new CacheEntry<>(Instant.now().toEpochMilli(), result));
        return result;
    }

    public List<FootballLeagueOption> fetchLeaguesForSeason(int season) {
        requireConfigured();
        JsonNode data = request("/leagues", Map.of("season", season));
        JsonNode rows = data.path("response");
        if (!rows.isArray()) {
            return List.of();
        }
        List<FootballLeagueOption> mapped = mapLeagueRows(rows);
        return filterTopLeagues(mapped);
    }

    public List<FootballTeamOption> fetchTeamsForLeague(int leagueId, int season) {
        requireConfigured();
        JsonNode data = request("/teams", Map.of("league", leagueId, "season", season));
        JsonNode rows = data.path("response");
        if (!rows.isArray()) {
            return List.of();
        }
        return mapTeamRows(rows);
    }

    private record CacheEntry<T>(long at, T data) {}

    private record Coords(double lat, double lng) {}

    private record TeamRow(
            String teamName,
            Integer venueId,
            String venueName,
            Coords embeddedCoords,
            String city,
            String country) {}

    private record VenuePoint(double lng, double lat, String venueName) {}

    private JsonNode request(String path, Map<String, ?> query) {
        RestClient.RequestHeadersSpec<?> spec =
                restClient.get().uri(uriBuilder -> {
                    var b = uriBuilder.path(path.startsWith("/") ? path : "/" + path);
                    query.forEach((k, v) -> b.queryParam(k, String.valueOf(v)));
                    return b.build();
                });
        String body;
        try {
            body = spec.retrieve().body(String.class);
        } catch (RestClientResponseException e) {
            throw toApiFootballException(e);
        } catch (Exception e) {
            throw new ApiFootballException("API-Football request failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY.value());
        }
        try {
            JsonNode data = objectMapper.readTree(body);
            JsonNode errors = data.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                for (JsonNode e : errors) {
                    if (!msg.isEmpty()) {
                        msg.append("; ");
                    }
                    msg.append(e.path("message").asText(String.valueOf(e)));
                }
                throw new ApiFootballException(msg.toString(), 422);
            }
            return data;
        } catch (ApiFootballException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiFootballException("Failed to parse API-Football response", 502);
        }
    }

    private static ApiFootballException toApiFootballException(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 429) {
            return new ApiFootballException(
                    "API-Football HTTP 429. API-Football rate limit reached. Wait a minute and try again.",
                    429);
        }
        if (status == 401 || status == 403) {
            return new ApiFootballException(
                    "API-Football rejected the API key (HTTP "
                            + status
                            + "). Set GitHub secret API_FOOTBALL_KEY to the key only (no API_FOOTBALL_KEY= prefix).",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }
        int mapped = status >= 400 && status < 600 ? status : HttpStatus.BAD_GATEWAY.value();
        return new ApiFootballException("API-Football HTTP " + status, mapped);
    }

    /** Strips accidental {@code API_FOOTBALL_KEY=} prefix from env/secret paste mistakes. */
    static String normalizeApiKey(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        final String prefix = "API_FOOTBALL_KEY=";
        if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return trimmed.substring(prefix.length()).trim();
        }
        return trimmed;
    }

    private String assertLeagueBelongsToTeam(int teamId, int leagueId, int season) {
        JsonNode data = request("/leagues", Map.of("team", teamId, "season", season));
        JsonNode rows = data.path("response");
        if (!rows.isArray()) {
            throw new ApiFootballException(
                    "League " + leagueId + " is not associated with team " + teamId + " for season " + season, 422);
        }
        for (JsonNode row : rows) {
            if (row.path("league").path("id").asInt(-1) == leagueId) {
                return row.path("league").path("name").asText("League " + leagueId);
            }
        }
        throw new ApiFootballException(
                "League " + leagueId + " is not associated with team " + teamId + " for season " + season, 422);
    }

    private TeamRow fetchTeam(int teamId) {
        JsonNode data = request("/teams", Map.of("id", teamId));
        JsonNode row = data.path("response").isArray() && data.path("response").size() > 0
                ? data.path("response").get(0)
                : null;
        if (row == null || row.path("team").isMissingNode()) {
            throw new ApiFootballException("Team " + teamId + " not found", 404);
        }
        JsonNode venue = row.path("venue");
        JsonNode team = row.path("team");
        Integer venueId = venue.path("id").isNumber() ? venue.path("id").asInt() : null;
        String venueName = textOr(venue.path("name"), textOr(team.path("name"), "Team " + teamId));
        String teamName = textOr(team.path("name"), "Team " + teamId);
        Coords embedded = extractCoords(venue);
        String city = textOr(venue.path("city"), textOr(team.path("city"), ""));
        String country = textOr(team.path("country"), "");
        return new TeamRow(teamName, venueId, venueName, embedded, city, country);
    }

    private VenuePoint fetchVenuePoint(int venueId) {
        JsonNode data = request("/venues", Map.of("id", venueId));
        JsonNode row = data.path("response").isArray() && data.path("response").size() > 0
                ? data.path("response").get(0)
                : null;
        Coords coords = row != null ? extractCoords(row) : null;
        if (coords == null) {
            throw new ApiFootballException("Venue " + venueId + " has no coordinates", 422);
        }
        String name = row != null ? row.path("name").asText("") : "";
        return new VenuePoint(coords.lng(), coords.lat(), name);
    }

    private Coords geocodeNominatim(String city, String country, String venueName) {
        String q = String.join(", ", List.of(venueName, city, country).stream().filter(s -> s != null && !s.isBlank()).toList());
        if (q.isBlank()) {
            throw new ApiFootballException("Cannot geocode venue: missing name and location hint", 422);
        }
        String url =
                "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
                        + java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8);
        try {
            String body =
                    RestClient.create()
                            .get()
                            .uri(url)
                            .header("Accept", "application/json")
                            .header("User-Agent", "DWSC-backend/1.0 (venue geocode fallback)")
                            .retrieve()
                            .body(String.class);
            JsonNode arr = objectMapper.readTree(body);
            if (!arr.isArray() || arr.isEmpty()) {
                throw new ApiFootballException(
                        "Venue coordinates unavailable from API-Football and geocoding found no match for: " + q, 422);
            }
            double lat = Double.parseDouble(arr.get(0).path("lat").asText());
            double lng = Double.parseDouble(arr.get(0).path("lon").asText());
            return new Coords(lat, lng);
        } catch (ApiFootballException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiFootballException("Geocoding failed", 502);
        }
    }

    private List<JsonNode> fetchAllPlayersPages(int teamId, int season) {
        List<JsonNode> all = new ArrayList<>();
        int page = 1;
        int totalPages = 1;
        while (page <= totalPages) {
            JsonNode data = request("/players", Map.of("team", teamId, "season", season, "page", page));
            JsonNode chunk = data.path("response");
            if (chunk.isArray()) {
                chunk.forEach(all::add);
            }
            totalPages = Math.max(1, data.path("paging").path("total").asInt(1));
            page++;
        }
        return all;
    }

    private ImportPlayerDoc mapImportRow(
            JsonNode row, String teamName, String leagueName, String venueName, GeoJsonPoint location) {
        JsonNode p = row.path("player");
        if (!p.has("id")) {
            return null;
        }
        int externalId = p.path("id").asInt();
        String position = "Unknown";
        if (row.path("statistics").isArray() && row.path("statistics").size() > 0) {
            position = row.path("statistics").get(0).path("games").path("position").asText("Unknown");
        } else if (p.has("position")) {
            position = p.path("position").asText("Unknown");
        }
        String name = p.path("name").asText("Player " + externalId);
        String image = p.path("photo").asText(null);
        JsonNode stats = row.has("statistics") ? row.path("statistics") : null;
        return new ImportPlayerDoc(name, teamName, leagueName, image, externalId, position, stats, venueName, location);
    }

    private List<FootballLeagueOption> mapLeagueRows(JsonNode rows) {
        Map<Integer, FootballLeagueOption> seen = new LinkedHashMap<>();
        for (JsonNode row : rows) {
            JsonNode league = row.path("league");
            if (!league.has("id")) {
                continue;
            }
            int id = league.path("id").asInt();
            if (seen.containsKey(id)) {
                continue;
            }
            String name = league.path("name").asText("League " + id);
            String logo = normalizeLogo(league.path("logo").asText(null));
            if (logo == null) {
                logo = normalizeLogo(row.path("country").path("flag").asText(null));
            }
            String country = row.path("country").path("name").asText(null);
            String type = league.path("type").asText(null);
            seen.put(id, new FootballLeagueOption(id, name, logo, country, type));
        }
        return seen.values().stream().sorted(Comparator.comparing(FootballLeagueOption::name)).toList();
    }

    private List<FootballLeagueOption> filterTopLeagues(List<FootballLeagueOption> leagues) {
        Map<Integer, FootballLeagueOption> byId = new LinkedHashMap<>();
        for (FootballLeagueOption l : leagues) {
            byId.put(l.id(), l);
        }
        List<FootballLeagueOption> result = new ArrayList<>();
        for (int id : TOP_LEAGUE_IDS) {
            FootballLeagueOption league = byId.get(id);
            if (league != null) {
                result.add(league);
            }
        }
        return result;
    }

    private List<FootballTeamOption> mapTeamRows(JsonNode rows) {
        Map<Integer, FootballTeamOption> seen = new LinkedHashMap<>();
        for (JsonNode row : rows) {
            JsonNode team = row.path("team");
            if (!team.has("id")) {
                continue;
            }
            int id = team.path("id").asInt();
            if (seen.containsKey(id)) {
                continue;
            }
            String name = team.path("name").asText("Team " + id);
            String logo = normalizeLogo(team.path("logo").asText(null));
            seen.put(id, new FootballTeamOption(id, name, logo));
        }
        return seen.values().stream().sorted(Comparator.comparing(FootballTeamOption::name)).toList();
    }

    private static Coords extractCoords(JsonNode o) {
        Double lat = parseCoord(o.path("lat").asText(null), "lat");
        Double lng = parseCoord(o.path("lng").asText(o.path("longitude").asText(o.path("lon").asText(null))), "lng");
        if (lat != null && lng != null) {
            return new Coords(lat, lng);
        }
        JsonNode c = o.path("coordinates");
        if (c.isArray() && c.size() >= 2) {
            Double lng2 = parseCoord(c.get(0).asText(), "lng");
            Double lat2 = parseCoord(c.get(1).asText(), "lat");
            if (lat2 != null && lng2 != null) {
                return new Coords(lat2, lng2);
            }
        }
        return null;
    }

    private static Double parseCoord(String raw, String kind) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        try {
            double n = Double.parseDouble(raw.trim());
            if (kind.equals("lat") && (n < -90 || n > 90)) {
                return null;
            }
            if (kind.equals("lng") && (n < -180 || n > 180)) {
                return null;
            }
            return n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeLogo(String url) {
        if (url == null || url.isBlank() || "null".equals(url)) {
            return null;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return null;
    }

    private static String textOr(JsonNode node, String fallback) {
        String t = node.asText("");
        return t.isBlank() ? fallback : t;
    }
}
