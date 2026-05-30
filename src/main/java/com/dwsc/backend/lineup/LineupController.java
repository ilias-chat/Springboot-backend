package com.dwsc.backend.lineup;

import com.dwsc.backend.api.dto.ErrorResponse;
import com.dwsc.backend.api.dto.LineupRequest;
import com.dwsc.backend.api.dto.LineupSuggestionResponse;
import com.dwsc.backend.auth.AuthSupport;
import com.dwsc.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lineup")
@Tag(name = "Lineup", description = "AI best-XI suggestions from the local player database")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class LineupController {

    private final LineupService lineupService;
    private final AuthSupport authSupport;

    public LineupController(LineupService lineupService, AuthSupport authSupport) {
        this.lineupService = lineupService;
        this.authSupport = authSupport;
    }

    @Operation(
            summary = "Suggest a best XI from locally stored players (AI)",
            description =
                    "Loads all players from the database and asks the configured AI provider (Groq or xAI Grok) "
                            + "to pick the best starting XI. Requires at least 25 players and GROK_API_KEY on the server.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "AI lineup suggestion"),
        @ApiResponse(
                responseCode = "400",
                description = "Missing or invalid formation",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Missing or invalid Firebase token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "User not found (sync account first)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Fewer than 25 players in the database",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "502",
                description = "AI provider error or invalid AI response",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "503",
                description = "GROK_API_KEY not configured",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/suggest")
    public LineupSuggestionResponse suggestLineup(
            @RequestBody(required = false) LineupRequest body, HttpServletRequest request) {
        authSupport.requireUser(request);
        String formation = body != null ? body.formation() : null;
        return lineupService.suggestLineup(formation);
    }
}
