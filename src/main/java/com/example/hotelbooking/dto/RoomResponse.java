package com.example.hotelbooking.dto;

import com.example.hotelbooking.model.RoomStatus;
import com.example.hotelbooking.model.RoomType;

import java.math.BigDecimal;

/**
 * Outgoing view of a room. Excludes the bookings collection so the API contract
 * stays decoupled from the JPA entity.
 */
public record RoomResponse(
        Long id,
        String roomNumber,
        RoomType type,
        BigDecimal pricePerNight,
        RoomStatus status
) {
}
