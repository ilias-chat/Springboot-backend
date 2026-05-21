package com.dwsc.backend.auth;

import com.dwsc.backend.model.entity.User;
import com.dwsc.backend.model.enums.UserRole;
import com.dwsc.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthSupport {

    public static final String ATTR_CURRENT_USER = "currentUser";

    private final UserRepository userRepository;

    public AuthSupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String requireFirebaseUid(HttpServletRequest request) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.ATTR_FIREBASE_UID);
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return uid;
    }

    public User requireUser(HttpServletRequest request) {
        User cached = (User) request.getAttribute(ATTR_CURRENT_USER);
        if (cached != null) {
            return cached;
        }
        String uid = requireFirebaseUid(request);
        User user =
                userRepository
                        .findByFirebaseUid(uid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        request.setAttribute(ATTR_CURRENT_USER, user);
        return user;
    }

    public User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (user.getRole() != UserRole.admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return user;
    }
}
