package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Persistence for Booking. Inherits CRUD from JpaRepository and adds the queries
 * that drive double-booking detection and the availability search.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * All non-cancelled bookings for a room. BookingService can use this to apply
     * the overlap predicate in Java, and the availability search uses it to decide
     * whether a room is free for a requested range.
     */
    List<Booking> findByRoomIdAndStatusNot(Long roomId, BookingStatus status);

    /**
     * Active bookings for a room that overlap the requested [checkIn, checkOut) range.
     *
     * Two ranges overlap when each starts before the other ends:
     *   existing.checkInDate  < :checkOut  AND  :checkIn < existing.checkOutDate
     *
     * CANCELLED bookings are excluded so a cancellation frees the dates again.
     * A non-empty result means the room is double-booked for that range (-> 409).
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status <> com.example.hotelbooking.model.BookingStatus.CANCELLED
              AND b.checkInDate < :checkOut
              AND :checkIn < b.checkOutDate
            """)
    List<Booking> findOverlapping(@Param("roomId") Long roomId,
                                  @Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);
}
