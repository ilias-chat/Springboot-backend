package com.dwsc.comment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddCommentRequest")
public record AddCommentRequest(
        String text,
        Integer rating,
        Double lat,
        Double lng) {}

