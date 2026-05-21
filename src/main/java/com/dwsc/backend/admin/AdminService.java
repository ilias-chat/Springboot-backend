package com.dwsc.backend.admin;

import com.dwsc.backend.api.dto.FootballLeagueOption;
import com.dwsc.backend.api.dto.FootballOptionsResponse;
import com.dwsc.backend.api.dto.FootballSquadPlayerOption;
import com.dwsc.backend.api.dto.FootballSquadPlayersResponse;
import com.dwsc.backend.api.dto.FootballTeamOption;
import com.dwsc.backend.api.dto.ImportPlayersRequest;
import com.dwsc.backend.api.dto.ImportPlayersResponse;
import com.dwsc.backend.api.dto.PlayerResponse;
import com.dwsc.backend.api.dto.UpdatePlayerRequest;
import com.dwsc.backend.football.ApiFootballException;
import com.dwsc.backend.football.ApiFootballService;
import com.dwsc.backend.football.ApiFootballService.ImportPlayerDoc;
import com.dwsc.backend.football.ApiFootballService.ImportPayloadResult;
import com.dwsc.backend.football.ApiFootballService.TeamStadiumContext;
import com.dwsc.backend.model.GeoJsonPoint;
import com.dwsc.backend.model.entity.Player;
import com.dwsc.backend.player.PlayerImageUtil;
import com.dwsc.backend.player.PlayerMapper;
import com.dwsc.backend.repository.PlayerRepository;
import com.dwsc.backend.util.GeoUtil;
import com.dwsc.backend.util.UuidValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminService {

    private static final Set<String> VALID_POSITIONS =
            Set.of("Attacker", "Midfielder", "Defender", "Goalkeeper");

    private final PlayerRepository playerRepository;
    private final ApiFootballService apiFootballService;

    public AdminService(PlayerRepository playerRepository, ApiFootballService apiFootballService) {
        this.playerRepository = playerRepository;
        this.apiFootballService = apiFootballService;
    }

    public FootballOptionsResponse<FootballLeagueOption> listLeagues(int season) {
        try {
            return new FootballOptionsResponse<>(apiFootballService.fetchLeaguesForSeason(season));
        } catch (ApiFootballException e) {
            throw e;
        }
    }

    public FootballOptionsResponse<FootballTeamOption> listTeams(int leagueId, int season) {
        try {
            return new FootballOptionsResponse<>(apiFootballService.fetchTeamsForLeague(leagueId, season));
        } catch (ApiFootballException e) {
            throw e;
        }
    }

    public FootballSquadPlayersResponse listSquadPlayers(int leagueId, int teamId, int season) {
        try {
            ImportPayloadResult payload = apiFootballService.buildSquadPreview(leagueId, teamId, season);
            List<FootballSquadPlayerOption> data =
                    payload.players().stream()
                            .map(
                                    p ->
                                            new FootballSquadPlayerOption(
                                                    p.externalId(), p.name(), p.position(), p.image()))
                            .toList();
            return new FootballSquadPlayersResponse(data, payload.teamName(), payload.leagueName());
        } catch (ApiFootballException e) {
            throw e;
        }
    }

    @Transactional
    public ImportPlayersResponse importPlayers(ImportPlayersRequest body) {
        if (body.leagueId() == null || body.teamId() == null || body.season() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leagueId, teamId, and season are required");
        }

        boolean useDeviceLocation = GeoUtil.hasDeviceCoords(body.lat(), body.lng());
        ImportPayloadResult payload;
        try {
            payload =
                    apiFootballService.buildImportPayloads(
                            body.leagueId(), body.teamId(), body.season(), useDeviceLocation);
        } catch (ApiFootballException e) {
            throw e;
        }

        List<ImportPlayerDoc> players = payload.players();
        GeoJsonPoint stadiumLocation =
                players.isEmpty() || players.get(0).location() == null ? null : players.get(0).location();
        GeoJsonPoint importLocation =
                GeoUtil.resolvePlayerLocation(stadiumLocation, body.lat(), body.lng(), useDeviceLocation);
        if (importLocation == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not resolve stadium coordinates for this team. "
                            + "Send lat and lng in the request body (device GPS), or enable location and try again.");
        }
        if (body.externalIds() != null) {
            if (body.externalIds().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "externalIds must be a non-empty array of player ids");
            }
            Set<Integer> idSet = new HashSet<>();
            for (Integer extId : body.externalIds()) {
                if (extId != null) {
                    idSet.add(extId);
                }
            }
            players =
                    players.stream().filter(doc -> idSet.contains(doc.externalId())).toList();
            if (players.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No matching players for the given externalIds");
            }
        }

        if (players.isEmpty()) {
            return new ImportPlayersResponse(
                    0,
                    0,
                    0,
                    payload.teamName(),
                    payload.leagueName(),
                    payload.venueName(),
                    null,
                    "No players returned for this squad");
        }

        int inserted = 0;
        int updated = 0;
        int matched = 0;
        for (ImportPlayerDoc doc : players) {
            ImportPlayerDoc toSave = withLocation(doc, importLocation);
            var existing = playerRepository.findByExternalId(doc.externalId());
            if (existing.isPresent()) {
                Player p = existing.get();
                applyImportSet(p, toSave);
                playerRepository.save(p);
                updated++;
                matched++;
            } else {
                Player p = new Player();
                applyImportSet(p, toSave);
                p.setRegistrationDate(Instant.now());
                playerRepository.save(p);
                inserted++;
            }
        }

        return new ImportPlayersResponse(
                inserted,
                updated,
                matched,
                payload.teamName(),
                payload.leagueName(),
                payload.venueName(),
                players.size(),
                null);
    }

    @Transactional
    public PlayerResponse updatePlayer(String id, UpdatePlayerRequest body) {
        UUID playerId = requirePlayerId(id);
        Player existing =
                playerRepository
                        .findById(playerId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        String trimmedName = body.name() != null ? body.name().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (body.position() == null || !VALID_POSITIONS.contains(body.position())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "position must be one of: Attacker, Midfielder, Defender, Goalkeeper");
        }
        if (body.lat() == null
                || body.lng() == null
                || body.lat() < -90
                || body.lat() > 90
                || body.lng() < -180
                || body.lng() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat and lng must be valid coordinates");
        }

        String normalizedImage = body.image() != null ? PlayerImageUtil.normalizeBase64Image(body.image()) : null;
        if (body.image() != null && !body.image().isBlank() && PlayerImageUtil.isInvalidSentinel(normalizedImage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image must be a valid base64 photo under 2MB");
        }

        String teamName = existing.getTeam();
        String leagueName = existing.getLeague();
        String venueName = existing.getVenueName();

        boolean hasTeamSelection =
                body.leagueId() != null && body.teamId() != null && body.season() != null;
        if (hasTeamSelection) {
            try {
                TeamStadiumContext ctx =
                        apiFootballService.resolveTeamStadiumContext(
                                body.leagueId(), body.teamId(), body.season());
                teamName = ctx.teamName();
                leagueName = ctx.leagueName();
                venueName = ctx.venueName();
            } catch (ApiFootballException e) {
                throw e;
            }
        }

        if (playerRepository.existsDuplicateExcluding(trimmedName, teamName, playerId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A player with this name already exists on this team");
        }

        existing.setName(trimmedName);
        existing.setPosition(body.position());
        existing.setTeam(teamName);
        existing.setLeague(leagueName);
        existing.setVenueName(venueName);
        existing.setLocation(new GeoJsonPoint("Point", List.of(body.lng(), body.lat())));
        if (normalizedImage != null) {
            existing.setImage(normalizedImage);
        }

        return PlayerMapper.toResponse(playerRepository.save(existing), false);
    }

    @Transactional
    public void deletePlayer(String id) {
        UUID playerId = requirePlayerId(id);
        if (!playerRepository.existsById(playerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }
        playerRepository.deleteById(playerId);
    }

    private static ImportPlayerDoc withLocation(ImportPlayerDoc doc, GeoJsonPoint location) {
        return new ImportPlayerDoc(
                doc.name(),
                doc.team(),
                doc.league(),
                doc.image(),
                doc.externalId(),
                doc.position(),
                doc.stats(),
                doc.venueName(),
                location);
    }

    private static void applyImportSet(Player player, ImportPlayerDoc doc) {
        player.setName(doc.name());
        player.setTeam(doc.team());
        player.setLeague(doc.league());
        player.setExternalId(doc.externalId());
        player.setPosition(doc.position());
        player.setStats(doc.stats());
        player.setVenueName(doc.venueName());
        player.setLocation(doc.location());
        if (doc.image() != null && !doc.image().isBlank()) {
            player.setImage(doc.image());
        }
    }

    private static UUID requirePlayerId(String id) {
        if (!UuidValidator.isValid(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player id");
        }
        return UuidValidator.parse(id);
    }
}
