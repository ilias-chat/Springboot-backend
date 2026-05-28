package com.dwsc.comment.comment;

import com.dwsc.comment.api.dto.AddCommentRequest;
import com.dwsc.comment.api.dto.AuthorCommentItem;
import com.dwsc.comment.api.dto.AuthorCommentsPageResponse;
import com.dwsc.comment.api.dto.CommentsListResponse;
import com.dwsc.comment.api.dto.PlayerCommentResponse;
import com.dwsc.comment.api.dto.PlayerCommentSummary;
import com.dwsc.comment.api.dto.PlayerCommentsSummaryResponse;
import com.dwsc.comment.auth.FirebaseAuthFilter;
import com.dwsc.comment.config.OpenApiConfig;
import com.dwsc.comment.model.GeoJsonPoint;
import com.dwsc.comment.model.entity.Comment;
import com.dwsc.comment.repository.CommentRepository;
import com.dwsc.comment.user.AuthorDisplayNameResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Comments", description = "Player comments and star ratings")
public class CommentController {

    private static final int MAX_SUMMARY_PLAYER_IDS = 100;

    private final CommentRepository commentRepository;
    private final AuthorDisplayNameResolver authorDisplayNameResolver;

    public CommentController(
            CommentRepository commentRepository, AuthorDisplayNameResolver authorDisplayNameResolver) {
        this.commentRepository = commentRepository;
        this.authorDisplayNameResolver = authorDisplayNameResolver;
    }

    @Operation(summary = "List comments for a player (public)")
    @GetMapping("/players/{playerId}/comments")
    public CommentsListResponse listComments(@PathVariable("playerId") String playerId) {
        UUID pid = requireUuid(playerId, "Invalid player id");
        List<PlayerCommentResponse> data =
                commentRepository.findByPlayerIdOrderByCreatedAtDesc(pid).stream()
                        .map(CommentMapper::toPlayerCommentResponse)
                        .toList();
        return new CommentsListResponse(data);
    }

    @Operation(summary = "Review count and average rating per player (public, for discovery lists)")
    @GetMapping("/players/comments-summary")
    public PlayerCommentsSummaryResponse summarizeByPlayerIds(@RequestParam("playerIds") String playerIds) {
        List<UUID> ids = parsePlayerIds(playerIds);
        if (ids.isEmpty()) {
            return new PlayerCommentsSummaryResponse(List.of());
        }
        List<PlayerCommentSummary> data =
                commentRepository.summarizeByPlayerIds(ids).stream()
                        .map(
                                row ->
                                        new PlayerCommentSummary(
                                                ((UUID) row[0]).toString(),
                                                ((Number) row[1]).longValue(),
                                                ((Number) row[2]).doubleValue()))
                        .toList();
        return new PlayerCommentsSummaryResponse(data);
    }

    @Operation(summary = "Add a comment and rating (requires Firebase token)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/players/{playerId}/comments")
    public ResponseEntity<PlayerCommentResponse> addComment(
            @PathVariable("playerId") String playerId,
            @RequestBody AddCommentRequest body,
            HttpServletRequest request) {
        UUID pid = requireUuid(playerId, "Invalid player id");
        String uid = requireUid(request);

        if (body == null || body.text() == null || body.text().isBlank()) {
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

        Comment c = new Comment();
        c.setPlayerId(pid);
        c.setAuthor(uid);
        c.setAuthorName(authorDisplayNameResolver.resolve(request));
        c.setText(body.text().trim());
        c.setRating(body.rating());
        c.setLocation(new GeoJsonPoint("Point", List.of(body.lng(), body.lat())));

        Comment saved = commentRepository.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentMapper.toPlayerCommentResponse(saved));
    }

    @Operation(summary = "Delete own comment (requires Firebase token)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/players/{playerId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("playerId") String playerId,
            @PathVariable("commentId") String commentId,
            HttpServletRequest request) {
        requireUuid(playerId, "Invalid player id");
        UUID cid = requireUuid(commentId, "Invalid comment id");
        String uid = requireUid(request);

        Comment c = commentRepository.findById(cid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!uid.equals(c.getAuthor())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        commentRepository.delete(c);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List current user's comments (requires Firebase token)")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/comments")
    public AuthorCommentsPageResponse listCommentsByAuthor(
            @RequestParam("author") String author,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        String uid = requireUid(request);
        if (!uid.equals(author)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        int safePage = Math.max(1, page);
        int safeLimit = Math.min(Math.max(1, limit), 50);
        var pageable = PageRequest.of(safePage - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        var rows = commentRepository.findByAuthorOrderByCreatedAtDesc(author, pageable);
        List<AuthorCommentItem> data = rows.getContent().stream().map(CommentMapper::toAuthorCommentItem).toList();
        long total = commentRepository.countByAuthor(author);
        return new AuthorCommentsPageResponse(data, safePage, safeLimit, total);
    }

    private static List<UUID> parsePlayerIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (ids.size() >= MAX_SUMMARY_PLAYER_IDS) {
                break;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ids.add(requireUuid(trimmed, "Invalid player id in playerIds"));
        }
        return ids;
    }

    private static UUID requireUuid(String raw, String message) {
        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static String requireUid(HttpServletRequest request) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_UID);
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return uid;
    }
}

