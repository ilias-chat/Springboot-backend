package com.dwsc.comment.auth;

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

public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String ATTR_FIREBASE_UID = "firebaseUid";
    public static final String ATTR_FIREBASE_NAME = "firebaseName";
    public static final String ATTR_FIREBASE_EMAIL = "firebaseEmail";

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
        if (uri.startsWith("/api/comments")) {
            // author-scoped query must be authenticated
            return false;
        }
        if (uri.matches("/api/players/[0-9a-fA-F\\-]{36}/comments") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return "GET".equalsIgnoreCase(request.getMethod());
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
            if (token.getName() != null && !token.getName().isBlank()) {
                request.setAttribute(ATTR_FIREBASE_NAME, token.getName());
            }
            if (token.getEmail() != null && !token.getEmail().isBlank()) {
                request.setAttribute(ATTR_FIREBASE_EMAIL, token.getEmail());
            }
            filterChain.doFilter(request, response);
        } catch (IllegalStateException e) {
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (FirebaseAuthException e) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        } finally {
            request.removeAttribute(ATTR_FIREBASE_UID);
            request.removeAttribute(ATTR_FIREBASE_NAME);
            request.removeAttribute(ATTR_FIREBASE_EMAIL);
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

