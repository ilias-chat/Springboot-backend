package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PaginatedPlayers")
public record PaginatedPlayersResponse(List<PlayerResponse> data, int page, int limit, long total) {}
