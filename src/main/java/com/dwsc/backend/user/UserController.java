package com.dwsc.backend.user;

import com.dwsc.backend.api.dto.PatchRoleBody;
import com.dwsc.backend.api.dto.UserResponse;
import com.dwsc.backend.auth.FirebaseAuthFilter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private static String requireUid(HttpServletRequest request) {
        return (String) request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_UID);
    }

    @PostMapping("/sync")
    public ResponseEntity<UserResponse> sync(@RequestBody JsonNode body, HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.sync(uid, body));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.login(uid));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.getMe(uid));
    }

    @GetMapping("/me/comments")
    public ResponseEntity<?> myComments(
            HttpServletRequest request,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.listMyComments(uid, page, limit));
    }

    @PutMapping("/{uid}")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable String uid, @RequestBody(required = false) JsonNode body, HttpServletRequest request) {
        String tokenUid = requireUid(request);
        return ResponseEntity.ok(userService.updateProfile(tokenUid, uid, body));
    }

    /** Admin-only: same as Node {@code GET /api/users/}. */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.listUsersForAdmin(uid));
    }

    @PatchMapping("/{uid}/role")
    public ResponseEntity<UserResponse> patchRole(
            @PathVariable String uid, @RequestBody(required = false) PatchRoleBody body, HttpServletRequest request) {
        String adminUid = requireUid(request);
        return ResponseEntity.ok(userService.patchRole(adminUid, uid, body));
    }
}
