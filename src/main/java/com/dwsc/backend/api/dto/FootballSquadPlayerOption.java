package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "FootballSquadPlayerOption")
public record FootballSquadPlayerOption(int externalId, String name, String position, String image) {}
