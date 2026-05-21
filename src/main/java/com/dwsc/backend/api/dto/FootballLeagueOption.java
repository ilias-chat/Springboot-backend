package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "FootballLeagueOption")
public record FootballLeagueOption(
        int id, String name, String logo, String country, String type) {}
