package com.dwsc.backend.comment;

import com.dwsc.backend.api.dto.PlayerCommentResponse;
import com.dwsc.backend.comment.dto.AuthorCommentsPageResponse;
import com.dwsc.backend.comment.dto.PlayerCommentSummary;
import com.dwsc.backend.comment.dto.PlayerCommentsSummaryResponse;
import com.dwsc.backend.model.entity.Player;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Resilient wrappers around {@link CommentClient} (Feign / Eureka failures must not break player reads). */
@Component
public class CommentClientSupport {

    private static final Logger log = LoggerFactory.getLogger(CommentClientSupport.class);

    private final CommentClient commentClient;

    public CommentClientSupport(CommentClient commentClient) {
        this.commentClient = commentClient;
    }

    public List<PlayerCommentResponse> listPlayerComments(UUID playerId) {
        try {
            var response = commentClient.listComments(playerId.toString());
            return response.data() != null ? response.data() : List.of();
        } catch (Exception ex) {
            log.warn("Failed to load comments for player {}: {}", playerId, ex.getMessage());
            return List.of();
        }
    }

    public AuthorCommentsPageResponse listCommentsByAuthor(String author, int page, int limit) {
        try {
            return commentClient.listCommentsByAuthor(author, page, limit);
        } catch (Exception ex) {
            log.warn("Failed to load comments for author {}: {}", author, ex.getMessage());
            return new AuthorCommentsPageResponse(List.of(), page, limit, 0);
        }
    }

    /** Batch review stats for discovery/map player lists (comment DB is separate from player DB). */
    public Map<UUID, PlayerCommentSummary> summarizeByPlayerIds(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return Map.of();
        }
        try {
            String ids =
                    players.stream().map(Player::getId).map(UUID::toString).collect(Collectors.joining(","));
            PlayerCommentsSummaryResponse response = commentClient.summarizeByPlayerIds(ids);
            if (response == null || response.data() == null) {
                return Map.of();
            }
            Map<UUID, PlayerCommentSummary> map = new HashMap<>();
            for (PlayerCommentSummary row : response.data()) {
                try {
                    map.put(UUID.fromString(row.playerId()), row);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid id
                }
            }
            return map;
        } catch (Exception ex) {
            log.warn("Failed to load comment summaries: {}", ex.getMessage());
            return Map.of();
        }
    }
}
