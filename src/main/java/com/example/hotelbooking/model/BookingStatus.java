package com.example.hotelbooking.model;

/**
 * Lifecycle of a booking. Normal flow: CONFIRMED -> CHECKED_IN -> CHECKED_OUT.
 * A booking may move to CANCELLED from any state. CANCELLED bookings are excluded
 * from the double-booking overlap check, which frees the dates again.
 */
public enum BookingStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
