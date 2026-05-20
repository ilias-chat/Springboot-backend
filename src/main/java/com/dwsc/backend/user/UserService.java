package com.dwsc.backend.user;

import com.dwsc.backend.api.dto.PatchRoleBody;
import com.dwsc.backend.api.dto.UserCommentEntryResponse;
import com.dwsc.backend.api.dto.UserCommentPlayerSummary;
import com.dwsc.backend.api.dto.UserCommentsListResponse;
import com.dwsc.backend.api.dto.UserResponse;
import com.dwsc.backend.model.entity.Player;
import com.dwsc.backend.model.entity.PlayerComment;
import com.dwsc.backend.model.entity.User;
import com.dwsc.backend.model.enums.UserRole;
import com.dwsc.backend.repository.PlayerCommentRepository;
import com.dwsc.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;
    private final PlayerCommentRepository playerCommentRepository;

    public UserService(UserRepository userRepository, PlayerCommentRepository playerCommentRepository) {
        this.userRepository = userRepository;
        this.playerCommentRepository = playerCommentRepository;
    }

    @Transactional
    public UserResponse sync(String tokenUid, JsonNode body) {
        JsonNode node = body != null && !body.isNull() ? body : JSON.objectNode();

        JsonNode firebaseNode = node.get("firebaseUID");
        if (firebaseNode == null || !firebaseNode.isTextual() || firebaseNode.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "firebaseUID is required");
        }
        String firebaseUID = firebaseNode.asText().trim();
        if (!firebaseUID.equals(tokenUid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "firebaseUID does not match token");
        }

        JsonNode emailNode = node.get("email");
        if (emailNode == null || !emailNode.isTextual() || emailNode.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }

        User user = userRepository.findByFirebaseUid(firebaseUID).orElseGet(User::new);
        if (user.getFirebaseUid() == null) {
            user.setFirebaseUid(firebaseUID);
        }
        user.setEmail(emailNode.asText().trim().toLowerCase());

        if (node.has("name")) {
            JsonNode n = node.get("name");
            if (n.isNull()) {
                // omit
            } else if (n.isTextual()) {
                user.setName(n.asText().trim());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must be a string when provided");
            }
        }

        if (node.has("avatar")) {
            JsonNode n = node.get("avatar");
            if (n.isNull() || (n.isTextual() && n.asText().isEmpty())) {
                user.setAvatar("");
            } else if (n.isTextual()) {
                user.setAvatar(n.asText().trim());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar must be a string, null, or empty string");
            }
        }

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public UserResponse login(String tokenUid) {
        User user =
                userRepository
                        .findByFirebaseUid(tokenUid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    public UserResponse getMe(String tokenUid) {
        User user =
                userRepository
                        .findByFirebaseUid(tokenUid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    public UserCommentsListResponse listMyComments(String tokenUid, Integer pageParam, Integer limitParam) {
        int page = parsePositiveInt(pageParam, DEFAULT_PAGE);
        int limit = Math.min(parsePositiveInt(limitParam, DEFAULT_LIMIT), MAX_LIMIT);
        var pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<PlayerComment> rows = playerCommentRepository.findByAuthorWithPlayer(tokenUid, pageable);
        long total = playerCommentRepository.countByAuthor(tokenUid);
        List<UserCommentEntryResponse> data = new ArrayList<>();
        for (PlayerComment c : rows) {
            Player p = c.getPlayer();
            data.add(
                    new UserCommentEntryResponse(
                            c.getId().toString(),
                            c.getText(),
                            c.getRating(),
                            c.getAuthor(),
                            c.getAuthorName(),
                            c.getCreatedAt(),
                            new UserCommentPlayerSummary(
                                    p.getId().toString(),
                                    p.getName(),
                                    p.getTeam(),
                                    p.getLeague(),
                                    p.getImage())));
        }
        return new UserCommentsListResponse(data, page, limit, total);
    }

    @Transactional
    public UserResponse updateProfile(String tokenUid, String pathUid, JsonNode body) {
        if (!pathUid.equals(tokenUid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        JsonNode node = body != null && !body.isNull() ? body : JSON.objectNode();

        boolean any = false;
        if (node.has("name")) {
            JsonNode n = node.get("name");
            if (n.isNull()) {
                // omit
            } else if (n.isTextual()) {
                any = true;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must be a string when provided");
            }
        }
        if (node.has("avatar")) {
            JsonNode n = node.get("avatar");
            if (n.isNull() || (n.isTextual() && n.asText().isEmpty())) {
                any = true;
            } else if (n.isTextual()) {
                any = true;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar must be a string, null, or empty string");
            }
        }
        if (!any) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No updatable fields provided");
        }

        User user =
                userRepository
                        .findByFirebaseUid(pathUid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (node.has("name") && !node.get("name").isNull()) {
            user.setName(node.get("name").asText().trim());
        }
        if (node.has("avatar")) {
            JsonNode n = node.get("avatar");
            if (n.isNull() || (n.isTextual() && n.asText().isEmpty())) {
                user.setAvatar("");
            } else if (n.isTextual()) {
                user.setAvatar(n.asText().trim());
            }
        }

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public List<UserResponse> listUsersForAdmin(String adminFirebaseUid) {
        User admin =
                userRepository
                        .findByFirebaseUid(adminFirebaseUid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (admin.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public UserResponse patchRole(String adminFirebaseUid, String targetUid, PatchRoleBody body) {
        User admin =
                userRepository
                        .findByFirebaseUid(adminFirebaseUid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (admin.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        String role = body != null ? body.role() : null;
        if (!"user".equals(role) && !"admin".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be user or admin");
        }
        if (admin.getFirebaseUid().equals(targetUid)
                && admin.getRole() == UserRole.admin
                && "user".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote yourself");
        }
        User target =
                userRepository
                        .findByFirebaseUid(targetUid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        target.setRole("admin".equals(role) ? UserRole.admin : UserRole.user);
        userRepository.save(target);
        return UserMapper.toResponse(target);
    }

    private static int parsePositiveInt(Integer v, int fallback) {
        if (v == null) {
            return fallback;
        }
        if (v < 1) {
            return fallback;
        }
        return v;
    }
}
