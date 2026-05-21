package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CommentsListResponse")
public record CommentsListResponse(List<PlayerCommentResponse> data) {}
