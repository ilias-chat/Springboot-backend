package com.dwsc.backend.football;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Mirrors TRWM-backend {@code ApiFootballError}. */
public class ApiFootballException extends ResponseStatusException {

    public ApiFootballException(String message, int statusCode) {
        super(HttpStatus.valueOf(statusCode), message);
    }

    public int getStatusCodeValue() {
        return getStatusCode().value();
    }
}
