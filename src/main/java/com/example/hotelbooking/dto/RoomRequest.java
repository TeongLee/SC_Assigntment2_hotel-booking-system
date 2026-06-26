package com.example.hotelbooking.dto;

import com.example.hotelbooking.model.RoomStatus;
import com.example.hotelbooking.model.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Incoming payload for creating or updating a room. Kept separate from the Room
 * entity so the public API stays decoupled from persistence. {@code status} is
 * optional on create and defaults to AVAILABLE in RoomService.
 */
public record RoomRequest(
        @NotBlank String roomNumber,
        @NotNull RoomType type,
        @NotNull @Positive BigDecimal pricePerNight,
        RoomStatus status
) {
}
