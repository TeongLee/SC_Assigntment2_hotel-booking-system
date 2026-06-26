package com.example.hotelbooking.dto;

import java.time.LocalDateTime;

/**
 * Consistent error body returned by GlobalExceptionHandler for every failure,
 * so clients see one predictable JSON shape instead of leaked stack traces.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
