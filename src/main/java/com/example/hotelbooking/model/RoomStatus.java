package com.example.hotelbooking.model;

/**
 * Whether a room can currently be booked. A room in MAINTENANCE is rejected by
 * BookingService even if its dates are free.
 */
public enum RoomStatus {
    AVAILABLE,
    MAINTENANCE
}
