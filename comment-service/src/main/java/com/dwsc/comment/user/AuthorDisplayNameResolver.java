package com.dwsc.comment.user;

import com.dwsc.comment.auth.FirebaseAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mirrors TRWM-backend {@code resolveAuthorDisplayName}: profile name, then email local-part, else Fan.
 */
@Component
public class AuthorDisplayNameResolver {

    private static final Logger log = LoggerFactory.getLogger(AuthorDisplayNameResolver.class);

    private final PlayerUserClient playerUserClient;

    public AuthorDisplayNameResolver(PlayerUserClient playerUserClient) {
        this.playerUserClient = playerUserClient;
    }

    public String resolve(HttpServletRequest request) {
        try {
            UserProfileResponse profile = playerUserClient.getMe();
            String fromProfile = resolveFromProfile(profile);
            if (fromProfile != null) {
                return fromProfile;
            }
        } catch (Exception ex) {
            log.debug("Could not load user profile for author name: {}", ex.getMessage());
        }

        String fromToken = resolveFromTokenAttributes(request);
        if (fromToken != null) {
            return fromToken;
        }
        return "Fan";
    }

    static String resolveFromProfile(UserProfileResponse profile) {
        if (profile == null) {
            return null;
        }
        String name = profile.name();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return emailLocalPart(profile.email());
    }

    static String resolveFromTokenAttributes(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object nameAttr = request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_NAME);
        if (nameAttr instanceof String name && !name.isBlank()) {
            return name.trim();
        }
        Object emailAttr = request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_EMAIL);
        if (emailAttr instanceof String email) {
            return emailLocalPart(email);
        }
        return null;
    }

    private static String emailLocalPart(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (!trimmed.contains("@")) {
            return null;
        }
        String local = trimmed.substring(0, trimmed.indexOf('@')).trim();
        return local.isEmpty() ? null : local;
    }
}
