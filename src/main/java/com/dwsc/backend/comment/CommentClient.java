package com.dwsc.backend.comment;

import com.dwsc.backend.api.dto.CommentsListResponse;
import com.dwsc.backend.comment.dto.AuthorCommentsPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "dwsc-comment")
public interface CommentClient {
    @GetMapping("/api/players/{playerId}/comments")
    CommentsListResponse listComments(@PathVariable("playerId") String playerId);

    @GetMapping("/api/comments")
    AuthorCommentsPageResponse listCommentsByAuthor(
            @RequestParam("author") String author,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit);
}

