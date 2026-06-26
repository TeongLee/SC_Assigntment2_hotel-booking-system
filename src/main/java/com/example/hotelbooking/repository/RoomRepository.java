package com.example.hotelbooking.repository;

import com.example.hotelbooking.model.Room;
import com.example.hotelbooking.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence for Room. Inherits CRUD from JpaRepository.
 */
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Used by the availability search when a RoomType filter is supplied.
    // When no type filter is given, RoomService falls back to findAll().
    List<Room> findByType(RoomType type);
}
