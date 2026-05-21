package com.dwsc.backend.player;

import com.dwsc.backend.api.dto.AddCommentRequest;
import com.dwsc.backend.api.dto.CommentsListResponse;
import com.dwsc.backend.api.dto.CreatePlayerRequest;
import com.dwsc.backend.api.dto.NearbyPlayersResponse;
import com.dwsc.backend.api.dto.NearbyPlayersResponse.StadiumSummary;
import com.dwsc.backend.api.dto.PaginatedPlayersResponse;
import com.dwsc.backend.api.dto.PlayerCommentResponse;
import com.dwsc.backend.api.dto.PlayerResponse;
import com.dwsc.backend.football.ApiFootballException;
import com.dwsc.backend.football.ApiFootballService;
import com.dwsc.backend.football.ApiFootballService.TeamStadiumContext;
import com.dwsc.backend.model.GeoJsonPoint;
import com.dwsc.backend.model.entity.Player;
import com.dwsc.backend.model.entity.PlayerComment;
import com.dwsc.backend.model.entity.User;
import com.dwsc.backend.model.enums.UserRole;
import com.dwsc.backend.repository.PlayerRepository;
import com.dwsc.backend.repository.UserRepository;
import com.dwsc.backend.util.EscapeRegex;
import com.dwsc.backend.util.UuidValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PlayerService {

    private static final Set<String> VALID_POSITIONS =
            Set.of("Attacker", "Midfielder", "Defender", "Goalkeeper");
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final Pattern REGISTERED_ON = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final ApiFootballService apiFootballService;

    public PlayerService(
            PlayerRepository playerRepository,
            UserRepository userRepository,
            ApiFootballService apiFootballService) {
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.apiFootballService = apiFootballService;
    }

    @Transactional(readOnly = true)
    public PaginatedPlayersResponse listPlayers(
            String team, String position, String q, String registeredOn, int page, int limit) {
        Instant regStart = null;
        Instant regEnd = null;
        if (registeredOn != null && !registeredOn.isBlank()) {
            var day = parseRegisteredOnDay(registeredOn.trim());
            if (day == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registeredOn; use YYYY-MM-DD");
            }
            regStart = day.start();
            regEnd = day.end();
        }
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("name").ascending());
        String teamParam = blankToNull(team);
        String positionParam = blankToNull(position);
        String qParam = blankToNull(q);
        Page<Player> result;
        if (teamParam == null && positionParam == null && qParam == null && regStart == null) {
            result = playerRepository.findAll(pageable);
        } else {
            result = playerRepository.findFiltered(teamParam, positionParam, qParam, regStart, regEnd, pageable);
        }
        return toPaginated(result, page, limit);
    }

    @Transactional(readOnly = true)
    public PaginatedPlayersResponse searchPlayers(String q, int page, int limit) {
        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter q is required");
        }
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("name").ascending());
        Page<Player> result = playerRepository.searchByName(EscapeRegex.escape(q.trim()), pageable);
        return toPaginated(result, page, limit);
    }

    @Transactional(readOnly = true)
    public PlayerResponse getPlayerById(String id) {
        UUID playerId = requirePlayerId(id);
        Player player =
                playerRepository
                        .findByIdWithComments(playerId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        return PlayerMapper.toResponse(player, true);
    }

    @Transactional(readOnly = true)
    public NearbyPlayersResponse nearbyPlayers(double lat, double lng, double radiusKm) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat or lng out of range");
        }
        if (radiusKm <= 0 || radiusKm > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "radiusKm must be between 0 and 5000");
        }
        List<Player> players = playerRepository.findNearby(lat, lng, radiusKm);
        List<PlayerResponse> playerResponses =
                players.stream().map(p -> PlayerMapper.toResponse(p, false)).toList();
        Map<String, StadiumSummary> stadiumMap = new LinkedHashMap<>();
        for (Player p : players) {
            if (p.getLocation() == null
                    || p.getLocation().getCoordinates() == null
                    || p.getLocation().getCoordinates().size() != 2) {
                continue;
            }
            String name = p.getVenueName() != null ? p.getVenueName() : (p.getTeam() != null ? p.getTeam() : "Unknown venue");
            double plng = p.getLocation().getCoordinates().get(0);
            double plat = p.getLocation().getCoordinates().get(1);
            String key = name + "|" + roundCoord(plng, 5) + "|" + roundCoord(plat, 5);
            stadiumMap.putIfAbsent(
                    key,
                    new StadiumSummary(name, new GeoJsonPoint("Point", List.of(plng, plat))));
        }
        return new NearbyPlayersResponse(playerResponses, List.copyOf(stadiumMap.values()));
    }

    @Transactional
    public PlayerResponse createPlayer(CreatePlayerRequest body) {
        String trimmedName = body.name() != null ? body.name().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (body.position() == null || !VALID_POSITIONS.contains(body.position())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "position must be one of: Attacker, Midfielder, Defender, Goalkeeper");
        }
        if (body.leagueId() == null || body.teamId() == null || body.season() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leagueId, teamId, and season must be numbers");
        }

        String normalizedImage = body.image() != null ? PlayerImageUtil.normalizeBase64Image(body.image()) : null;
        if (body.image() != null && !body.image().isBlank() && PlayerImageUtil.isInvalidSentinel(normalizedImage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image must be a valid base64 photo under 2MB");
        }

        boolean useDeviceLocation = hasDeviceCoords(body.lat(), body.lng());
        TeamStadiumContext ctx;
        try {
            ctx =
                    apiFootballService.resolveTeamStadiumContext(
                            body.leagueId(), body.teamId(), body.season(), useDeviceLocation);
        } catch (ApiFootballException e) {
            throw e;
        }

        GeoJsonPoint location = resolvePlayerLocation(ctx.location(), body.lat(), body.lng(), useDeviceLocation);
        if (location == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not resolve stadium coordinates for this team. "
                            + "Send lat and lng in the request body (device GPS), or enable location and try again.");
        }

        if (playerRepository.existsByNameIgnoreCaseAndTeamIgnoreCase(trimmedName, ctx.teamName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A player with this name already exists on this team");
        }

        Player player = new Player();
        player.setName(trimmedName);
        player.setPosition(body.position());
        player.setTeam(ctx.teamName());
        player.setLeague(ctx.leagueName());
        player.setVenueName(ctx.venueName());
        player.setLocation(location);
        player.setRegistrationDate(Instant.now());
        if (normalizedImage != null) {
            player.setImage(normalizedImage);
        }
        return PlayerMapper.toResponse(playerRepository.save(player), false);
    }

    @Transactional(readOnly = true)
    public CommentsListResponse listComments(String playerId) {
        UUID id = requirePlayerId(playerId);
        Player player =
                playerRepository
                        .findByIdWithComments(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        List<PlayerCommentResponse> data =
                player.getComments().stream().map(PlayerMapper::toCommentResponse).toList();
        return new CommentsListResponse(data);
    }

    @Transactional
    public PlayerCommentResponse addComment(String playerId, String firebaseUid, AddCommentRequest body) {
        UUID id = requirePlayerId(playerId);
        if (body.text() == null || body.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        if (body.rating() == null || body.rating() < 0 || body.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be a number between 0 and 5");
        }
        if (body.lat() == null
                || body.lng() == null
                || body.lat() < -90
                || body.lat() > 90
                || body.lng() < -180
                || body.lng() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat and lng must be finite numbers");
        }

        Player player =
                playerRepository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
        String authorName = resolveAuthorDisplayName(user, firebaseUid);

        PlayerComment comment = new PlayerComment();
        comment.setPlayer(player);
        comment.setAuthor(firebaseUid);
        comment.setAuthorName(authorName);
        comment.setText(body.text().trim());
        comment.setRating(body.rating().intValue());
        comment.setLocation(new GeoJsonPoint("Point", List.of(body.lng(), body.lat())));

        player.getComments().add(comment);
        playerRepository.save(player);
        return PlayerMapper.toCommentResponse(comment);
    }

    @Transactional
    public void assertCommentDeleteAllowed(String playerId, String commentId, String firebaseUid) {
        UUID pid = requirePlayerId(playerId);
        UUID cid = requireCommentId(commentId);
        Player player =
                playerRepository
                        .findByIdWithComments(pid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        PlayerComment comment =
                player.getComments().stream()
                        .filter(c -> c.getId().equals(cid))
                        .findFirst()
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (firebaseUid.equals(comment.getAuthor())) {
            return;
        }
        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
        if (user != null && user.getRole() == UserRole.admin) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    @Transactional
    public void deleteComment(String playerId, String commentId) {
        UUID pid = requirePlayerId(playerId);
        UUID cid = requireCommentId(commentId);
        Player player =
                playerRepository
                        .findByIdWithComments(pid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        boolean removed = player.getComments().removeIf(c -> c.getId().equals(cid));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
        playerRepository.save(player);
    }

    public static int parsePositiveInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int n = Integer.parseInt(raw.trim());
            return n >= 1 ? n : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int parseLimit(String raw) {
        return Math.min(parsePositiveInt(raw, DEFAULT_LIMIT), MAX_LIMIT);
    }

    public static double parseRequiredDouble(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, label + " must be a finite number");
        }
        try {
            double n = Double.parseDouble(raw.trim());
            if (!Double.isFinite(n)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, label + " must be a finite number");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, label + " must be a finite number");
        }
    }

    public static Double parseOptionalRadiusKm(String radiusKm, String distanceMeters) {
        if (radiusKm != null && !radiusKm.isBlank()) {
            double r = parseRequiredDouble(radiusKm, "radiusKm");
            return r;
        }
        if (distanceMeters != null && !distanceMeters.isBlank()) {
            double d = parseRequiredDouble(distanceMeters, "distance");
            if (d > 0) {
                return d / 1000.0;
            }
        }
        return null;
    }

    private PaginatedPlayersResponse toPaginated(Page<Player> page, int pageNum, int limit) {
        List<PlayerResponse> data =
                page.getContent().stream().map(p -> PlayerMapper.toResponse(p, false)).toList();
        return new PaginatedPlayersResponse(data, pageNum, limit, page.getTotalElements());
    }

    private static UUID requirePlayerId(String id) {
        if (!UuidValidator.isValid(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player id");
        }
        return UuidValidator.parse(id);
    }

    private static UUID requireCommentId(String id) {
        if (!UuidValidator.isValid(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id");
        }
        return UuidValidator.parse(id);
    }

    private record DayRange(Instant start, Instant end) {}

    private static DayRange parseRegisteredOnDay(String raw) {
        var m = REGISTERED_ON.matcher(raw);
        if (!m.matches()) {
            return null;
        }
        int y = Integer.parseInt(m.group(1));
        int mo = Integer.parseInt(m.group(2));
        int d = Integer.parseInt(m.group(3));
        if (mo < 1 || mo > 12 || d < 1 || d > 31) {
            return null;
        }
        LocalDate date = LocalDate.of(y, mo, d);
        if (date.getYear() != y || date.getMonthValue() != mo || date.getDayOfMonth() != d) {
            return null;
        }
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.atTime(23, 59, 59, 999_000_000).atZone(ZoneOffset.UTC).toInstant();
        return new DayRange(start, end);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static boolean hasDeviceCoords(Double lat, Double lng) {
        return lat != null
                && lng != null
                && Double.isFinite(lat)
                && Double.isFinite(lng)
                && lat >= -90
                && lat <= 90
                && lng >= -180
                && lng <= 180;
    }

    /** Stadium coords from API-Football when present; otherwise device GPS when {@code useDeviceLocation}. */
    static GeoJsonPoint resolvePlayerLocation(
            GeoJsonPoint stadiumLocation, Double lat, Double lng, boolean useDeviceLocation) {
        if (stadiumLocation != null) {
            return stadiumLocation;
        }
        if (useDeviceLocation) {
            return new GeoJsonPoint("Point", List.of(lng, lat));
        }
        return null;
    }

    private static double roundCoord(double n, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(n * f) / f;
    }

    private static String resolveAuthorDisplayName(User user, String firebaseUid) {
        if (user != null && user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        String email = user != null && user.getEmail() != null ? user.getEmail().trim() : "";
        if (!email.isEmpty() && email.contains("@")) {
            return email.split("@")[0];
        }
        return "Fan";
    }
}
