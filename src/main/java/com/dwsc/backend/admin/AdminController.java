package com.dwsc.backend.admin;

import com.dwsc.backend.api.dto.ErrorResponse;
import com.dwsc.backend.api.dto.FootballLeagueOption;
import com.dwsc.backend.api.dto.FootballOptionsResponse;
import com.dwsc.backend.api.dto.FootballSquadPlayersResponse;
import com.dwsc.backend.api.dto.FootballTeamOption;
import com.dwsc.backend.api.dto.ImportPlayersRequest;
import com.dwsc.backend.api.dto.ImportPlayersResponse;
import com.dwsc.backend.api.dto.PlayerResponse;
import com.dwsc.backend.api.dto.UpdatePlayerRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "API-Football helpers and player maintenance")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminController {

    private final AdminService adminService;
    private final AuthSupport authSupport;

    public AdminController(AdminService adminService, AuthSupport authSupport) {
        this.adminService = adminService;
        this.authSupport = authSupport;
    }

    @Operation(summary = "List leagues for a season (API-Football)")
    @GetMapping("/leagues")
    public FootballOptionsResponse<FootballLeagueOption> listLeagues(
            @RequestParam int season, HttpServletRequest request) {
        authSupport.requireUser(request);
        return adminService.listLeagues(season);
    }

    @Operation(summary = "List teams for a league and season (API-Football)")
    @GetMapping("/teams")
    public FootballOptionsResponse<FootballTeamOption> listTeams(
            @RequestParam int leagueId, @RequestParam int season, HttpServletRequest request) {
        authSupport.requireUser(request);
        return adminService.listTeams(leagueId, season);
    }

    @Operation(summary = "Squad roster preview for import UI (API-Football)")
    @GetMapping("/squad-players")
    public FootballSquadPlayersResponse listSquadPlayers(
            @RequestParam int leagueId,
            @RequestParam int teamId,
            @RequestParam int season,
            HttpServletRequest request) {
        authSupport.requireUser(request);
        return adminService.listSquadPlayers(leagueId, teamId, season);
    }

    @Operation(summary = "Import squad from API-Football into the database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bulk upsert summary"),
        @ApiResponse(
                responseCode = "400",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/import-players")
    public ImportPlayersResponse importPlayers(
            @RequestBody ImportPlayersRequest body, HttpServletRequest request) {
        authSupport.requireUser(request);
        return adminService.importPlayers(body);
    }

    @Operation(summary = "Update a player (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated player"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/players/{id}")
    public PlayerResponse updatePlayer(
            @PathVariable String id, @RequestBody UpdatePlayerRequest body, HttpServletRequest request) {
        authSupport.requireAdmin(request);
        return adminService.updatePlayer(id, body);
    }

    @Operation(summary = "Remove a player from the local database (admin)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/players/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable String id, HttpServletRequest request) {
        authSupport.requireAdmin(request);
        adminService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
