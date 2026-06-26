package com.example.hotelbooking.service;

import com.example.hotelbooking.dto.RoomRequest;
import com.example.hotelbooking.dto.RoomResponse;
import com.example.hotelbooking.exception.ResourceNotFoundException;
import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomStatus;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Room CRUD plus the availability search (functionality #5). Holds no HTTP concerns;
 * controllers translate these results into responses.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<RoomResponse> listAll() {
        return roomRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RoomResponse getById(Long id) {
        return toResponse(findRoomOrThrow(id));
    }

    public RoomResponse create(RoomRequest request) {
        Room room = new Room();
        room.setRoomNumber(request.roomNumber());
        room.setType(request.type());
        room.setPricePerNight(request.pricePerNight());
        // status is optional on create; default to AVAILABLE.
        room.setStatus(request.status() != null ? request.status() : RoomStatus.AVAILABLE);
        return toResponse(roomRepository.save(room));
    }

    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findRoomOrThrow(id);
        room.setRoomNumber(request.roomNumber());
        room.setType(request.type());
        room.setPricePerNight(request.pricePerNight());
        if (request.status() != null) {
            room.setStatus(request.status());
        }
        return toResponse(roomRepository.save(room));
    }

    public void delete(Long id) {
        Room room = findRoomOrThrow(id);
        roomRepository.delete(room);
    }

    /**
     * Search rooms free for the requested range (functionality #5, showcase endpoint).
     * A room qualifies when it is AVAILABLE and has no overlapping active booking.
     * An optional RoomType narrows the candidate set.
     */
    public List<RoomResponse> searchAvailable(LocalDate checkIn, LocalDate checkOut, RoomType type) {
        validateRange(checkIn, checkOut);

        List<Room> candidates = (type != null)
                ? roomRepository.findByType(type)
                : roomRepository.findAll();

        return candidates.stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> bookingRepository
                        .findOverlapping(room.getId(), checkIn, checkOut).isEmpty())
                .map(this::toResponse)
                .toList();
    }

    private void validateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("checkIn and checkOut are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be strictly after checkIn");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("checkIn cannot be in the past");
        }
    }

    private Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room " + id + " not found"));
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getType(),
                room.getPricePerNight(),
                room.getStatus()
        );
    }
}
