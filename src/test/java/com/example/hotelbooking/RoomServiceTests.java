package com.example.hotelbooking;

import com.example.hotelbooking.dto.RoomRequest;
import com.example.hotelbooking.dto.RoomResponse;
import com.example.hotelbooking.exception.BookingConflictException;
import com.example.hotelbooking.model.RoomStatus;
import com.example.hotelbooking.model.RoomType;
import com.example.hotelbooking.repository.RoomRepository;
import com.example.hotelbooking.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RoomServiceTests {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void deleteRoom_withExistingBookings_isRejected() {
        Long roomId = roomRepository.findAll().stream()
                .filter(room -> room.getRoomNumber().equals("101"))
                .findFirst()
                .orElseThrow()
                .getId();

        assertThatThrownBy(() -> roomService.delete(roomId))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("cannot be deleted because it has bookings");
    }

    @Test
    void deleteRoom_withoutBookings_removesRoom() {
        RoomResponse room = roomService.create(new RoomRequest(
                "909",
                RoomType.SINGLE,
                new BigDecimal("180"),
                RoomStatus.AVAILABLE
        ));

        roomService.delete(room.id());

        assertThat(roomRepository.existsById(room.id())).isFalse();
    }
}
