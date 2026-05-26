package com.dwsc.comment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AuthorCommentItem")
public record AuthorCommentItem(
        String id,
        String playerId,
        String text,
        int rating,
        String author,
        String authorName,
        Instant createdAt) {}

