package com.example.hotelbooking.service;

import com.example.hotelbooking.dto.BookingRequest;
import com.example.hotelbooking.dto.BookingResponse;
import com.example.hotelbooking.exception.BookingConflictException;
import com.example.hotelbooking.exception.ResourceNotFoundException;
import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.BookingStatus;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomStatus;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Booking lifecycle and the business rules that make this more than flat CRUD:
 * double-booking detection, maintenance/date guards, and server-side pricing.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    public List<BookingResponse> listAll() {
        return bookingRepository.findAll().stream().map(this::toResponse).toList();
    }

    public BookingResponse getById(Long id) {
        return toResponse(findBookingOrThrow(id));
    }

    /**
     * Create a booking (functionality #1, showcase endpoint). Validates the room
     * exists and is bookable, the dates are sane, and no active booking overlaps,
     * then computes totalPrice on the server.
     */
    public BookingResponse createBooking(BookingRequest request) {
        Room room = findRoomOrThrow(request.roomId());
        validateBookable(room, request.checkInDate(), request.checkOutDate());
        ensureNoOverlap(room.getId(), request.checkInDate(), request.checkOutDate(), null);

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuestName(request.guestName());
        booking.setGuestEmail(request.guestEmail());
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalPrice(computeTotal(room, request.checkInDate(), request.checkOutDate()));

        return toResponse(bookingRepository.save(booking));
    }

    /**
     * Update a booking (functionality #3). Re-validates the dates and re-checks for
     * overlaps, excluding this booking from its own conflict check.
     */
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = findBookingOrThrow(id);
        Room room = findRoomOrThrow(request.roomId());
        validateBookable(room, request.checkInDate(), request.checkOutDate());
        ensureNoOverlap(room.getId(), request.checkInDate(), request.checkOutDate(), id);

        booking.setRoom(room);
        booking.setGuestName(request.guestName());
        booking.setGuestEmail(request.guestEmail());
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setTotalPrice(computeTotal(room, request.checkInDate(), request.checkOutDate()));

        return toResponse(bookingRepository.save(booking));
    }

    public void delete(Long id) {
        Booking booking = findBookingOrThrow(id);
        bookingRepository.delete(booking);
    }

    // --- business rules ---

    private void validateBookable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new BookingConflictException(
                    "Room " + room.getRoomNumber() + " is under maintenance and cannot be booked");
        }
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("checkInDate and checkOutDate are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOutDate must be strictly after checkInDate");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkInDate cannot be in the past");
        }
    }

    /**
     * Reject the request if any active (non-cancelled) booking on this room overlaps
     * the requested range. When updating, {@code excludeBookingId} keeps a booking from
     * conflicting with itself.
     */
    private void ensureNoOverlap(Long roomId, LocalDate checkIn, LocalDate checkOut,
                                 Long excludeBookingId) {
        boolean conflict = bookingRepository.findOverlapping(roomId, checkIn, checkOut).stream()
                .anyMatch(b -> excludeBookingId == null || !b.getId().equals(excludeBookingId));
        if (conflict) {
            throw new BookingConflictException(
                    "Room is already booked for " + checkIn + " to " + checkOut);
        }
    }

    /** totalPrice = nights * pricePerNight, computed server-side; never from the client. */
    private BigDecimal computeTotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room " + id + " not found"));
    }

    private Booking findBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + id + " not found"));
    }

    private BookingResponse toResponse(Booking booking) {
        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        Room room = booking.getRoom();
        return new BookingResponse(
                booking.getId(),
                room.getRoomNumber(),
                room.getType(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                nights,
                booking.getTotalPrice(),
                booking.getStatus()
        );
    }
}
