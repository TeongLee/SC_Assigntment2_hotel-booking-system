package com.example.hotelbooking.config;

import com.example.hotelbooking.model.Booking;
import com.example.hotelbooking.model.BookingStatus;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomStatus;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Seeds the in-memory database on startup so the API, availability search, and
 * overlap demos return meaningful data immediately. Runs only when empty, so a
 * restart with a persisted DB would not duplicate rows.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public DataSeeder(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void run(String... args) {
        if (roomRepository.count() > 0) {
            return;
        }

        Room r101 = roomRepository.save(room("101", RoomType.SINGLE, "150", RoomStatus.AVAILABLE));
        Room r102 = roomRepository.save(room("102", RoomType.DOUBLE, "250", RoomStatus.AVAILABLE));
        roomRepository.save(room("201", RoomType.SUITE, "450", RoomStatus.AVAILABLE));
        roomRepository.save(room("202", RoomType.DELUXE, "600", RoomStatus.AVAILABLE));
        roomRepository.save(room("301", RoomType.SINGLE, "150", RoomStatus.MAINTENANCE));

        // Two seed bookings (dates relative to today so they are always valid).
        LocalDate today = LocalDate.now();
        bookingRepository.save(booking(r101, "Liam Chen", "liam@example.com",
                today.plusDays(3), today.plusDays(6)));
        bookingRepository.save(booking(r102, "Sara Lim", "sara@example.com",
                today.plusDays(10), today.plusDays(13)));
    }

    private Room room(String number, RoomType type, String price, RoomStatus status) {
        Room room = new Room();
        room.setRoomNumber(number);
        room.setType(type);
        room.setPricePerNight(new BigDecimal(price));
        room.setStatus(status);
        return room;
    }

    private Booking booking(Room room, String guest, String email,
                            LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuestName(guest);
        booking.setGuestEmail(email);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setStatus(BookingStatus.CONFIRMED);
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        booking.setTotalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        return booking;
    }
}
