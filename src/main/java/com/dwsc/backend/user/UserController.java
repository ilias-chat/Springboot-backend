package com.dwsc.backend.user;

import com.dwsc.backend.api.dto.ErrorResponse;
import com.dwsc.backend.api.dto.PatchRoleBody;
import com.dwsc.backend.api.dto.SyncUserBody;
import com.dwsc.backend.api.dto.UpdateProfileBody;
import com.dwsc.backend.api.dto.UserCommentsListResponse;
import com.dwsc.backend.api.dto.UserResponse;
import com.dwsc.backend.auth.FirebaseAuthFilter;
import com.dwsc.backend.config.OpenApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "Profiles, auth exchange, admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private static String requireUid(HttpServletRequest request) {
        return (String) request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_UID);
    }

    @Operation(
            summary = "Create or update profile after Firebase registration",
            description = "Upserts the user row. Body firebaseUID must match the Firebase ID token uid.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saved user", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "firebaseUID does not match token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/sync")
    public ResponseEntity<UserResponse> sync(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = SyncUserBody.class)))
                    @RequestBody
                    JsonNode body,
            HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.sync(uid, body));
    }

    @Operation(summary = "Token exchange — return user including role")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not synced yet", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.login(uid));
    }

    @Operation(summary = "Get current user profile and permissions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.getMe(uid));
    }

    @Operation(summary = "List scout reports (comments) authored by the current user")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Paginated comments with player summary",
                content = @Content(schema = @Schema(implementation = UserCommentsListResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me/comments")
    public ResponseEntity<UserCommentsListResponse> myComments(
            HttpServletRequest request,
            @Parameter(description = "Page number (1-based)") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size (max 50)") @RequestParam(value = "limit", required = false) Integer limit) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.listMyComments(uid, page, limit));
    }

    @Operation(summary = "Update own profile (name, avatar)", description = "Path uid must be the caller's Firebase UID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated user", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "No updatable fields", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "uid does not match caller", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{uid}")
    public ResponseEntity<UserResponse> updateProfile(
            @Parameter(description = "Firebase UID (must match token)") @PathVariable("uid") String uid,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(implementation = UpdateProfileBody.class)))
                    @RequestBody(required = false)
                    JsonNode body,
            HttpServletRequest request) {
        String tokenUid = requireUid(request);
        return ResponseEntity.ok(userService.updateProfile(tokenUid, uid, body));
    }

    @Operation(summary = "List all users (admin only)")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Array of users",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Admin user record not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(HttpServletRequest request) {
        String uid = requireUid(request);
        return ResponseEntity.ok(userService.listUsersForAdmin(uid));
    }

    @Operation(summary = "Change a user's role (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated user", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid role or self-demotion blocked", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{uid}/role")
    public ResponseEntity<UserResponse> patchRole(
            @Parameter(description = "Target user's Firebase UID") @PathVariable("uid") String uid,
            @RequestBody(required = false) PatchRoleBody body,
            HttpServletRequest request) {
        String adminUid = requireUid(request);
        return ResponseEntity.ok(userService.patchRole(adminUid, uid, body));
    }
}
