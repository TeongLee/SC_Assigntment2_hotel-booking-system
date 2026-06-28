package com.example.hotelbooking.dto;

import com.example.hotelbooking.model.BookingStatus;
import com.example.hotelbooking.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Outgoing view of a booking. Flattens the Room relationship to roomId/roomNumber/roomType
 * and exposes the server-computed nights and totalPrice, hiding JPA internals.
 */
public record BookingResponse(
        Long id,
        Long roomId,
        String roomNumber,
        RoomType roomType,
        String guestName,
        String guestEmail,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        BigDecimal totalPrice,
        BookingStatus status
) {
}
