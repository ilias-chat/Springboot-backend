package com.dwsc.backend.controller;

import com.dwsc.backend.api.dto.HelloResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "System", description = "Health and root")
public class HealthController {

    @Operation(summary = "API root", description = "Welcome payload")
    @ApiResponse(responseCode = "200", description = "Welcome payload", content = @Content(schema = @Schema(implementation = HelloResponse.class)))
    @GetMapping("/")
    public HelloResponse root() {
        return new HelloResponse("Hello World");
    }

    @Operation(summary = "Liveness probe")
    @ApiResponse(responseCode = "200", description = "Service is up", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "ok")))
    @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
