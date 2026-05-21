package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "FootballSquadPlayersResponse")
public record FootballSquadPlayersResponse(
        List<FootballSquadPlayerOption> data, String teamName, String leagueName) {}
