package com.dwsc.comment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlayerCommentSummary")
public record PlayerCommentSummary(String playerId, long count, double avgRating) {}
