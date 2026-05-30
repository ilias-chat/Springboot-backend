package com.dwsc.backend.api.dto;

/** Body for {@code POST /api/lineup/suggest}. */
public record LineupRequest(String formation) {}
