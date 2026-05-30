package com.dwsc.backend.lineup;

import com.dwsc.backend.api.dto.LineupSlotResponse;
import com.dwsc.backend.api.dto.LineupSuggestionResponse;
import com.dwsc.backend.comment.CommentClientSupport;
import com.dwsc.backend.comment.dto.PlayerCommentSummary;
import com.dwsc.backend.lineup.AiLineupClient.LineupPlayerInput;
import com.dwsc.backend.lineup.AiLineupClient.LineupSuggestion;
import com.dwsc.backend.lineup.AiLineupClient.SlotPick;
import com.dwsc.backend.model.entity.Player;
import com.dwsc.backend.repository.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI best-XI suggestion over the full local player database.
 * Port of TRWM-backend {@code services/lineupService.js}.
 */
@Service
public class LineupService {

    static final int MIN_PLAYERS_FOR_LINEUP = 25;
    static final int MAX_ROSTER_FOR_AI = 100;
    static final Set<String> VALID_FORMATIONS =
            Set.of("4-4-2", "4-3-3", "3-5-2", "4-2-3-1", "3-4-3", "5-3-2", "4-5-1");

    private final PlayerRepository playerRepository;
    private final CommentClientSupport commentClient;
    private final AiLineupClient aiLineupClient;

    public LineupService(
            PlayerRepository playerRepository,
            CommentClientSupport commentClient,
            AiLineupClient aiLineupClient) {
        this.playerRepository = playerRepository;
        this.commentClient = commentClient;
        this.aiLineupClient = aiLineupClient;
    }

    @Transactional(readOnly = true)
    public LineupSuggestionResponse suggestLineup(String formationRaw) {
        String formation = formationRaw != null ? formationRaw.trim() : "";
        if (formation.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "formation is required");
        }
        if (!VALID_FORMATIONS.contains(formation)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid formation. Use one of: " + String.join(", ", VALID_FORMATIONS));
        }

        long total = playerRepository.count();
        if (total < MIN_PLAYERS_FOR_LINEUP) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Need at least " + MIN_PLAYERS_FOR_LINEUP + " players in the local database. Found " + total + ".");
        }

        List<Player> players =
                playerRepository
                        .findAll(PageRequest.of(0, MAX_ROSTER_FOR_AI, Sort.by("name").ascending()))
                        .getContent();
        Map<UUID, PlayerCommentSummary> summaries = commentClient.summarizeByPlayerIds(players);

        List<LineupPlayerInput> roster = new ArrayList<>(players.size());
        Map<String, Player> pool = new LinkedHashMap<>();
        Map<String, PlayerCommentSummary> summaryByIdStr = new LinkedHashMap<>();
        for (Player p : players) {
            String idStr = p.getId().toString();
            PlayerCommentSummary summary = summaries.get(p.getId());
            pool.put(idStr, p);
            summaryByIdStr.put(idStr, summary);
            roster.add(toInput(p, idStr, summary));
        }

        LineupSuggestion suggestion = aiLineupClient.suggestLineup(roster, formation);

        List<LineupSlotResponse> starters =
                enrich(suggestion.starters(), "starters", pool, summaryByIdStr, new HashSet<>());
        if (starters.size() != 11) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned " + starters.size() + " starters; expected exactly 11.");
        }
        Set<String> used = new HashSet<>();
        starters.forEach(s -> used.add(s.playerId()));
        List<LineupSlotResponse> bench = enrich(suggestion.bench(), "bench", pool, summaryByIdStr, used);

        String formationOut =
                suggestion.formation() != null && !suggestion.formation().isBlank()
                        ? suggestion.formation()
                        : formation;
        return new LineupSuggestionResponse(formationOut, suggestion.reasoning(), total, roster.size(), starters, bench);
    }

    private List<LineupSlotResponse> enrich(
            List<SlotPick> picks,
            String label,
            Map<String, Player> pool,
            Map<String, PlayerCommentSummary> summaryByIdStr,
            Set<String> used) {
        List<LineupSlotResponse> out = new ArrayList<>();
        if (picks == null) {
            return out;
        }
        int index = 0;
        for (SlotPick pick : picks) {
            Player player = pool.get(pick.playerId());
            if (player == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI picked unknown playerId \"" + pick.playerId() + "\" in " + label + "[" + index + "].");
            }
            if (!used.add(pick.playerId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Duplicate playerId \"" + pick.playerId() + "\" in lineup.");
            }
            PlayerCommentSummary summary = summaryByIdStr.get(pick.playerId());
            String role = pick.role() != null && !pick.role().isBlank() ? pick.role() : player.getPosition();
            out.add(
                    new LineupSlotResponse(
                            pick.playerId(),
                            pick.slot(),
                            role,
                            player.getName(),
                            player.getTeam(),
                            player.getPosition(),
                            summary != null ? summary.avgRating() : null,
                            player.getImage()));
            index++;
        }
        return out;
    }

    private LineupPlayerInput toInput(Player player, String idStr, PlayerCommentSummary summary) {
        Double avgRating = summary != null ? summary.avgRating() : null;
        long reviewCount = summary != null ? summary.count() : 0L;
        return new LineupPlayerInput(
                idStr,
                player.getName(),
                player.getTeam(),
                player.getPosition(),
                avgRating,
                reviewCount,
                summarizeStats(player.getStats()));
    }

    static String summarizeStats(JsonNode stats) {
        if (stats == null || stats.isNull() || !stats.isObject()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        JsonNode goals = stats.path("goals");
        JsonNode total = goals.path("total");
        if (total.isNumber()) {
            parts.add("goals:" + total.asText());
        }
        JsonNode assists = goals.path("assists");
        if (assists.isNumber()) {
            parts.add("assists:" + assists.asText());
        }
        JsonNode games = stats.path("games");
        JsonNode apps = games.path("appearences").isMissingNode() ? games.path("appearances") : games.path("appearences");
        if (apps.isNumber()) {
            parts.add("apps:" + apps.asText());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
