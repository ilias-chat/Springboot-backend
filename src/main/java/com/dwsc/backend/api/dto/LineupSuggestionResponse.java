package com.dwsc.backend.api.dto;

import java.util.List;

/** AI lineup suggestion — mirrors TRWM-backend {@code services/lineupService.js} response shape. */
public record LineupSuggestionResponse(
        String formation,
        String reasoning,
        long playerCount,
        int rosterSentToAi,
        List<LineupSlotResponse> starters,
        List<LineupSlotResponse> bench) {}
