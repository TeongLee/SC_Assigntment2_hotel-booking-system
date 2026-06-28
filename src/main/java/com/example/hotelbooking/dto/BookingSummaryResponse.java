package com.example.hotelbooking.dto;

import java.math.BigDecimal;

/**
 * Aggregated booking totals for a lightweight reporting endpoint.
 */
public record BookingSummaryResponse(
        long totalBookings,
        long confirmedBookings,
        long checkedInBookings,
        long checkedOutBookings,
        long cancelledBookings,
        long upcomingBookings,
        BigDecimal totalRevenue
) {
}
