package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "FootballOptionsResponse")
public record FootballOptionsResponse<T>(List<T> data) {}
