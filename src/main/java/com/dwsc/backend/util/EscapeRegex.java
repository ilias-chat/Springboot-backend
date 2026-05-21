package com.dwsc.backend.util;

public final class EscapeRegex {

    private EscapeRegex() {}

    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("([\\\\.^$|?*+()\\[\\]{}])", "\\\\$1");
    }
}
