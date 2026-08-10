package com.fris.boardportal.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> fieldErrors) {

    public ErrorResponse(int status, String error, String message) {
        this(Instant.now(), status, error, message, List.of());
    }

    public ErrorResponse(int status, String error, String message, List<String> fieldErrors) {
        this(Instant.now(), status, error, message, fieldErrors);
    }
}
