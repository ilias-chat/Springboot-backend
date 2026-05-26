package com.dwsc.backend.player;

import com.dwsc.backend.api.dto.CreatePlayerRequest;
import com.dwsc.backend.api.dto.ErrorResponse;
import com.dwsc.backend.api.dto.NearbyPlayersResponse;
import com.dwsc.backend.api.dto.PaginatedPlayersResponse;
import com.dwsc.backend.api.dto.PlayerResponse;
import com.dwsc.backend.auth.FirebaseAuthFilter;
import com.dwsc.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "Player directory, search, geo, comments")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Operation(summary = "List players with optional filters")
    @ApiResponse(responseCode = "200", description = "Paginated players")
    @GetMapping
    public PaginatedPlayersResponse listPlayers(
            @RequestParam(value = "team", required = false) String team,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "registeredOn", required = false) String registeredOn,
            @RequestParam(value = "page", required = false, defaultValue = "1") String page,
            @RequestParam(value = "limit", required = false, defaultValue = "20") String limit) {
        return playerService.listPlayers(
                team,
                position,
                q,
                registeredOn,
                PlayerService.parsePositiveInt(page, 1),
                PlayerService.parseLimit(limit));
    }

    @Operation(summary = "Text search by player name")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated players"),
        @ApiResponse(
                responseCode = "400",
                description = "Missing q",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public PaginatedPlayersResponse searchPlayers(
            @RequestParam("q") String q,
            @RequestParam(value = "page", required = false, defaultValue = "1") String page,
            @RequestParam(value = "limit", required = false, defaultValue = "20") String limit) {
        return playerService.searchPlayers(
                q, PlayerService.parsePositiveInt(page, 1), PlayerService.parseLimit(limit));
    }

    @Operation(summary = "Players and stadiums within radius (public)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Players and deduped stadiums"),
        @ApiResponse(
                responseCode = "400",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/nearby")
    public NearbyPlayersResponse nearbyPlayers(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "distance", required = false) Double distance) {
        Double radius = radiusKm;
        if (radius == null) {
            radius = PlayerService.parseOptionalRadiusKm(null, distance != null ? String.valueOf(distance) : null);
        }
        if (radius == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "lat, lng, and radiusKm (or distance in meters) must be finite numbers");
        }
        return playerService.nearbyPlayers(lat, lng, radius);
    }

    @Operation(summary = "Player detail including stats and location")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Full player document"),
        @ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public PlayerResponse getPlayerById(@PathVariable("id") String id) {
        return playerService.getPlayerById(id);
    }

    @Operation(summary = "Create a player (requires Firebase token)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created player"),
        @ApiResponse(
                responseCode = "401",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@RequestBody CreatePlayerRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.createPlayer(body));
    }

    private static String requireUid(HttpServletRequest request) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_UID);
        if (uid == null || uid.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }
        return uid;
    }
}
