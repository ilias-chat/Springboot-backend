package com.dwsc.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HelloResponse")
public record HelloResponse(@Schema(example = "Hello World") String message) {}
