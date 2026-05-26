package com.dwsc.backend.comment.dto;

import java.time.Instant;

public record AuthorCommentItem(
        String id,
        String playerId,
        String text,
        int rating,
        String author,
        String authorName,
        Instant createdAt) {}

