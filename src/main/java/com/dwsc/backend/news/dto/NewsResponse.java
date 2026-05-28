package com.dwsc.backend.news.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "News")
public record NewsResponse(String id, String title, String content, String date) {}
