package com.example.hotelbooking;

import com.example.hotelbooking.dto.BookingRequest;
import com.example.hotelbooking.dto.BookingResponse;
import com.example.hotelbooking.exception.BookingConflictException;
import com.example.hotelbooking.exception.InvalidBookingStateException;
import com.example.hotelbooking.exception.ResourceNotFoundException;
import com.example.hotelbooking.model.BookingStatus;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service-level tests for the booking business rules and the status lifecycle.
 * Runs against the seeded in-memory database; @Transactional rolls back each test
 * so they stay isolated and never pollute the seed data other tests rely on.
 */
@SpringBootTest
@Transactional
class BookingServiceTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomRepository roomRepository;

    // Room 201 (SUITE, RM450/night) has no seed booking, so it is free for these tests.
    private Long availableRoomId() {
        return roomByNumber("201").getId();
    }

    private Room roomByNumber(String number) {
        return roomRepository.findAll().stream()
                .filter(r -> r.getRoomNumber().equals(number))
                .findFirst()
                .orElseThrow();
    }

    private BookingRequest request(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return new BookingRequest(roomId, "Test Guest", "guest@example.com", checkIn, checkOut);
    }

    // --- business rules ---

    @Test
    void createBooking_computesPriceServerSideAndConfirms() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        BookingResponse booking = bookingService.createBooking(
                request(availableRoomId(), checkIn, checkIn.plusDays(3)));

        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.roomId()).isEqualTo(availableRoomId());
        assertThat(booking.nights()).isEqualTo(3);
        // 3 nights * RM450 = RM1350, computed on the server (never sent by the client).
        assertThat(booking.totalPrice()).isEqualByComparingTo(new BigDecimal("1350"));
    }

    @Test
    void createBooking_rejectsOverlappingDatesWithConflict() {
        LocalDate checkIn = LocalDate.now().plusDays(40);
        Long roomId = availableRoomId();
        bookingService.createBooking(request(roomId, checkIn, checkIn.plusDays(3)));

        // A second booking on the same room whose range overlaps the first -> 409.
        assertThatThrownBy(() ->
                bookingService.createBooking(request(roomId, checkIn.plusDays(1), checkIn.plusDays(4))))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void createBooking_rejectsCheckoutBeforeCheckin() {
        LocalDate checkIn = LocalDate.now().plusDays(50);
        assertThatThrownBy(() ->
                bookingService.createBooking(request(availableRoomId(), checkIn, checkIn.minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBooking_rejectsMissingRoom() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        assertThatThrownBy(() ->
                bookingService.createBooking(request(9999L, checkIn, checkIn.plusDays(2))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBooking_rejectsRoomUnderMaintenance() {
        // Room 301 is seeded as MAINTENANCE and must not be bookable.
        LocalDate checkIn = LocalDate.now().plusDays(10);
        assertThatThrownBy(() ->
                bookingService.createBooking(request(roomByNumber("301").getId(), checkIn, checkIn.plusDays(2))))
                .isInstanceOf(BookingConflictException.class);
    }

    // --- status lifecycle ---

    @Test
    void lifecycle_checkInThenCheckOut() {
        LocalDate checkIn = LocalDate.now().plusDays(60);
        BookingResponse created = bookingService.createBooking(
                request(availableRoomId(), checkIn, checkIn.plusDays(2)));

        assertThat(bookingService.checkIn(created.id()).status()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(bookingService.checkOut(created.id()).status()).isEqualTo(BookingStatus.CHECKED_OUT);
    }

    @Test
    void checkOut_withoutCheckIn_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(70);
        BookingResponse created = bookingService.createBooking(
                request(availableRoomId(), checkIn, checkIn.plusDays(2)));

        assertThatThrownBy(() -> bookingService.checkOut(created.id()))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void cancel_freesTheDatesForRebooking() {
        LocalDate checkIn = LocalDate.now().plusDays(80);
        Long roomId = availableRoomId();
        BookingResponse first = bookingService.createBooking(request(roomId, checkIn, checkIn.plusDays(3)));

        // Cancelling soft-deletes the booking, so the same dates become bookable again.
        bookingService.cancel(first.id());
        BookingResponse rebooked = bookingService.createBooking(request(roomId, checkIn, checkIn.plusDays(3)));

        assertThat(rebooked.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancel_checkedOutBooking_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(90);
        BookingResponse created = bookingService.createBooking(
                request(availableRoomId(), checkIn, checkIn.plusDays(2)));
        bookingService.checkIn(created.id());
        bookingService.checkOut(created.id());

        assertThatThrownBy(() -> bookingService.cancel(created.id()))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void update_cancelledBooking_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(100);
        Long roomId = availableRoomId();
        BookingResponse created = bookingService.createBooking(
                request(roomId, checkIn, checkIn.plusDays(2)));
        bookingService.cancel(created.id());

        assertThatThrownBy(() ->
                bookingService.updateBooking(created.id(), request(roomId, checkIn.plusDays(1), checkIn.plusDays(3))))
                .isInstanceOf(InvalidBookingStateException.class)
                .hasMessageContaining("Cannot update");
    }

    @Test
    void update_checkedOutBooking_isRejected() {
        LocalDate checkIn = LocalDate.now().plusDays(110);
        Long roomId = availableRoomId();
        BookingResponse created = bookingService.createBooking(
                request(roomId, checkIn, checkIn.plusDays(2)));
        bookingService.checkIn(created.id());
        bookingService.checkOut(created.id());

        assertThatThrownBy(() ->
                bookingService.updateBooking(created.id(), request(roomId, checkIn.plusDays(1), checkIn.plusDays(3))))
                .isInstanceOf(InvalidBookingStateException.class)
                .hasMessageContaining("Cannot update");
    }
}
