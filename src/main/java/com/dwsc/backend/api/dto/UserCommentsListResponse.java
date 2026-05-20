package com.dwsc.backend.api.dto;

import java.util.List;

/** Matches Ionic {@code UserCommentsResponse}. */
public record UserCommentsListResponse(List<UserCommentEntryResponse> data, int page, int limit, long total) {}
