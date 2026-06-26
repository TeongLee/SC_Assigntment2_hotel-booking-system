package com.example.hotelbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Incoming payload for creating or updating a booking. Validated with @Valid in the
 * controller. The cross-field rule (checkOutDate strictly after checkInDate) and the
 * totalPrice are enforced/computed in BookingService, not here.
 */
public record BookingRequest(
        @NotNull Long roomId,
        @NotBlank String guestName,
        @NotBlank @Email String guestEmail,
        @NotNull @FutureOrPresent LocalDate checkInDate,
        @NotNull LocalDate checkOutDate
) {
}
