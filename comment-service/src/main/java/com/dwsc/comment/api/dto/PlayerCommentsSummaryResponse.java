package com.dwsc.comment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PlayerCommentsSummaryResponse")
public record PlayerCommentsSummaryResponse(List<PlayerCommentSummary> data) {}
