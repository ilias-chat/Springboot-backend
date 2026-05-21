package com.dwsc.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ImportPlayersResponse")
public record ImportPlayersResponse(
        Integer inserted,
        Integer updated,
        Integer matched,
        String teamName,
        String leagueName,
        String venueName,
        Integer playersProcessed,
        String message) {}
