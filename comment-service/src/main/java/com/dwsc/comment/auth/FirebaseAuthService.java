package com.dwsc.comment.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "dwsc.security.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseAuthService {

    private final Object lock = new Object();

    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        ensureFirebaseInitialized();
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }

    private void ensureFirebaseInitialized() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        synchronized (lock) {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }
            String raw = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException(
                        "FIREBASE_SERVICE_ACCOUNT_JSON is not set. Add the same secret as TRWM-backend to your env.");
            }
            try {
                var cred =
                        GoogleCredentials.fromStream(
                                new ByteArrayInputStream(raw.trim().getBytes(StandardCharsets.UTF_8)));
                FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(cred).build());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize Firebase: " + e.getMessage(), e);
            }
        }
    }
}

