package com.dwsc.backend.comment;

import com.dwsc.backend.api.dto.CommentsListResponse;
import com.dwsc.backend.comment.dto.AuthorCommentsPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cloud Run: set {@code COMMENT_SERVICE_URL} to the comment-service HTTPS base (no trailing slash).
 * When unset, resolves instances via Eureka (local/docker).
 */
@FeignClient(
        name = "dwsc-comment",
        url = "${COMMENT_SERVICE_URL:}",
        configuration = FeignAuthForwardingConfig.class)
public interface CommentClient {
    @GetMapping("/api/players/{playerId}/comments")
    CommentsListResponse listComments(@PathVariable("playerId") String playerId);

    @GetMapping("/api/comments")
    AuthorCommentsPageResponse listCommentsByAuthor(
            @RequestParam("author") String author,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit);
}

