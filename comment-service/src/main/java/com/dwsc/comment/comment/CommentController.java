package com.dwsc.comment.comment;

import com.dwsc.comment.api.dto.AddCommentRequest;
import com.dwsc.comment.api.dto.AuthorCommentItem;
import com.dwsc.comment.api.dto.AuthorCommentsPageResponse;
import com.dwsc.comment.api.dto.CommentsListResponse;
import com.dwsc.comment.api.dto.PlayerCommentResponse;
import com.dwsc.comment.auth.FirebaseAuthFilter;
import com.dwsc.comment.model.GeoJsonPoint;
import com.dwsc.comment.model.entity.Comment;
import com.dwsc.comment.repository.CommentRepository;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentRepository commentRepository;

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @GetMapping("/players/{playerId}/comments")
    public CommentsListResponse listComments(@PathVariable String playerId) {
        UUID pid = requireUuid(playerId, "Invalid player id");
        List<PlayerCommentResponse> data =
                commentRepository.findByPlayerIdOrderByCreatedAtDesc(pid).stream()
                        .map(CommentMapper::toPlayerCommentResponse)
                        .toList();
        return new CommentsListResponse(data);
    }

    @PostMapping("/players/{playerId}/comments")
    public ResponseEntity<PlayerCommentResponse> addComment(
            @PathVariable String playerId,
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
        c.setAuthorName(null);
        c.setText(body.text().trim());
        c.setRating(body.rating());
        c.setLocation(new GeoJsonPoint("Point", List.of(body.lng(), body.lat())));

        Comment saved = commentRepository.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentMapper.toPlayerCommentResponse(saved));
    }

    @DeleteMapping("/players/{playerId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String playerId,
            @PathVariable String commentId,
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

    @GetMapping("/comments")
    public AuthorCommentsPageResponse listCommentsByAuthor(
            @RequestParam String author,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
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

