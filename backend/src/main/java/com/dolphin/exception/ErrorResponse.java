package com.dolphin.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Uniform error body returned for every failed request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        String timestamp,
        String path,
        Map<String, String> errors
) {

    public static ErrorResponse of(int status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(int status, String message, String path, Map<String, String> errors) {
        String timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        return new ErrorResponse(status, message, timestamp, path, errors);
    }
}
