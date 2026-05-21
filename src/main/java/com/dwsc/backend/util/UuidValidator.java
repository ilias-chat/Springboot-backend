package com.dwsc.backend.util;

import java.util.UUID;

public final class UuidValidator {

    private UuidValidator() {}

    public static boolean isValid(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(id.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static UUID parse(String id) {
        return UUID.fromString(id.trim());
    }
}
