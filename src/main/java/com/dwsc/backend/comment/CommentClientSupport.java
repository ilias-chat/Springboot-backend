package com.dwsc.backend.comment;

import com.dwsc.backend.api.dto.PlayerCommentResponse;
import com.dwsc.backend.comment.dto.AuthorCommentItem;
import com.dwsc.backend.comment.dto.AuthorCommentsPageResponse;
import java.util.List;
import java.util.UUID;
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
}
