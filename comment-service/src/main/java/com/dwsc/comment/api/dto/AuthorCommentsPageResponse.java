package com.dwsc.comment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "AuthorCommentsPageResponse")
public record AuthorCommentsPageResponse(
        List<AuthorCommentItem> data,
        int page,
        int limit,
        long total) {}

