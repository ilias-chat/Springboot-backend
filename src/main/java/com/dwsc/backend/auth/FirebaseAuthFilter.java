package com.dwsc.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Bearer Firebase ID token → request attribute {@link #ATTR_FIREBASE_UID}. Same rules as TRWM-backend
 * {@code authMiddleware.verifyFirebaseToken}.
 */
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String ATTR_FIREBASE_UID = "firebaseUid";

    private final FirebaseAuthService firebaseAuthService;

    public FirebaseAuthFilter(FirebaseAuthService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/users") || uri.startsWith("/api/admin")) {
            return false;
        }
        if (uri.startsWith("/api/players") && "GET".equalsIgnoreCase(request.getMethod())) {
            return isPublicPlayerGet(uri);
        }
        return !uri.startsWith("/api/players");
    }

    /** Public GET routes — same as TRWM-backend playerRoutes (no auth middleware). */
    private static boolean isPublicPlayerGet(String uri) {
        if ("/api/players".equals(uri)
                || "/api/players/search".equals(uri)
                || "/api/players/nearby".equals(uri)) {
            return true;
        }
        if (uri.matches("/api/players/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return true;
        }
        return uri.matches(
                "/api/players/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/comments");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }
        String idToken = header.substring(7).trim();
        if (idToken.startsWith("Bearer ")) {
            idToken = idToken.substring(7).trim();
        }
        if (idToken.isEmpty()) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }
        try {
            var token = firebaseAuthService.verifyIdToken(idToken);
            request.setAttribute(ATTR_FIREBASE_UID, token.getUid());
            filterChain.doFilter(request, response);
        } catch (IllegalStateException e) {
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (FirebaseAuthException e) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            request.removeAttribute(ATTR_FIREBASE_UID);
        }
    }

    private static void writeJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(MAPPER.writeValueAsString(Map.of("error", message)));
    }
}
