package com.example.hotelbooking.exception;

/**
 * Thrown when a booking cannot be made because it overlaps an existing active
 * booking for the same room (double-booking). Mapped to HTTP 409 by
 * GlobalExceptionHandler.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
