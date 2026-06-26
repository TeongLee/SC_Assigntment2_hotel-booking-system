package com.example.hotelbooking.exception;

/**
 * Thrown when a requested entity (room or booking) does not exist.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
