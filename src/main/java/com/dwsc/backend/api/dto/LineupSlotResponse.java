package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** One enriched lineup slot (starter or bench) returned to the client. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LineupSlotResponse(
        String playerId,
        String slot,
        String role,
        String name,
        String team,
        String position,
        Double avgRating,
        String image) {}
