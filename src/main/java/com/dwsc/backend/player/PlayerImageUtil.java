package com.dwsc.backend.player;

/** Base64 / data-URL image normalization — same rules as TRWM-backend player controllers. */
public final class PlayerImageUtil {

    private static final int MAX_IMAGE_CHARS = 2_800_000;

    private PlayerImageUtil() {}

    /**
     * @return normalized data URL, {@code null} if invalid/too large, empty optional as {@code ""} means clear
     */
    public static String normalizeBase64Image(Object image) {
        if (image == null) {
            return null;
        }
        String s = String.valueOf(image).trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() > MAX_IMAGE_CHARS) {
            return INVALID;
        }
        if (s.startsWith("data:image/")) {
            return s;
        }
        if (s.matches("^[A-Za-z0-9+/=]+$")) {
            return "data:image/jpeg;base64," + s;
        }
        return INVALID;
    }

    /** Sentinel returned when image was provided but invalid. */
    public static final String INVALID = "__INVALID__";

    public static boolean isInvalidSentinel(String value) {
        return INVALID.equals(value);
    }
}
