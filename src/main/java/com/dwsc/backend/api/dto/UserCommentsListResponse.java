package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Matches Ionic {@code UserCommentsResponse}. */
@Schema(name = "UserCommentsListResponse")
public record UserCommentsListResponse(
        List<UserCommentEntryResponse> data, int page, int limit, long total) {}
