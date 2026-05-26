package com.dwsc.backend.comment.dto;

import java.util.List;

public record AuthorCommentsPageResponse(List<AuthorCommentItem> data, int page, int limit, long total) {}

