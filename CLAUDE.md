# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

Hotel Room Booking System — SECJ4383 Software Construction Assignment 2. A Spring Boot REST API with an in-memory H2 database. The design spec is in [docs/hotel-booking-design-blueprint.md](docs/hotel-booking-design-blueprint.md) and the phased build guide is in [docs/hotel-booking-build-guide.md](docs/hotel-booking-build-guide.md). These are the authoritative source of truth for every design decision.

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Build (compile + package)
./mvnw package

# Compile only
./mvnw compile

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=HotelbookingApplicationTests
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

After startup: API at `http://localhost:8080/api/rooms`, H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:hoteldb`, user: `sa`, no password).

## Architecture

Strict four-layer package structure under `com.example.hotelbooking`:

```
controller/   — HTTP layer only; delegates to services; returns DTOs
service/      — all business logic (overlap detection, price calculation, validation)
repository/   — JpaRepository extensions; custom overlap query lives in BookingRepository
model/        — JPA entities (Room, Booking) + enums (RoomType, RoomStatus, BookingStatus)
dto/          — Java records: BookingRequest (inbound + @Valid), BookingResponse, RoomResponse, ErrorResponse
exception/    — ResourceNotFoundException (→ 404), BookingConflictException (→ 409), GlobalExceptionHandler
config/       — DataSeeder (CommandLineRunner that seeds 5 rooms + 2 bookings on startup)
```

Controllers never expose JPA entities directly — always map through DTOs. `totalPrice` is always computed server-side as `nights × pricePerNight`; never accepted from the client.

## Key business rules

- **No double-booking**: overlap is detected in `BookingService` via a `BookingRepository` query for non-CANCELLED bookings on the same room where `existing.checkIn < new.checkOut AND new.checkIn < existing.checkOut`. Conflict → 409.
- **Room status**: booking a `MAINTENANCE` room is rejected. Only `AVAILABLE` rooms can be booked.
- **Date validation**: `checkOutDate` must be strictly after `checkInDate`; `checkInDate` cannot be past. Enforced by Bean Validation annotations on `BookingRequest` plus a service-level guard.
- **Cancellation**: a `CANCELLED` booking is excluded from overlap checks, freeing the dates.

## REST endpoints

Two showcase endpoints required by the assignment: `POST /api/bookings` (create booking) and `GET /api/rooms/available?checkIn=&checkOut=&type=` (search). All five functionalities are exposed as REST:

| Method | URL | Status |
|--------|-----|--------|
| GET/POST | `/api/rooms` | 200/201 |
| GET/PUT/DELETE | `/api/rooms/{id}` | 200/204 |
| GET | `/api/rooms/available` | 200 |
| GET/POST | `/api/bookings` | 200/201 |
| GET/PUT/DELETE | `/api/bookings/{id}` | 200/204 |

## application.properties (required config)

```properties
spring.datasource.url=jdbc:h2:mem:hoteldb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.jpa.show-sql=true
```

## DTOs (all in `dto/` package, implemented as Java records)

| Record | Direction | Fields |
|--------|-----------|--------|
| `BookingRequest` | inbound (`@Valid`) | `roomId (Long)`, `guestName (@NotBlank)`, `guestEmail (@Email)`, `checkInDate (@NotNull @FutureOrPresent)`, `checkOutDate (@NotNull)` |
| `BookingResponse` | outbound | `id`, `roomNumber`, `roomType`, `guestName`, `guestEmail`, `checkInDate`, `checkOutDate`, `nights`, `totalPrice`, `status` |
| `RoomResponse` | outbound | `id`, `roomNumber`, `type`, `pricePerNight`, `status` |
| `ErrorResponse` | error body | `timestamp`, `status`, `error`, `message`, `path` |

Controllers accept `BookingRequest` / plain fields and always return these response records — never JPA entities.

## Enums

- **RoomType**: `SINGLE`, `DOUBLE`, `SUITE`, `DELUXE`
- **RoomStatus**: `AVAILABLE`, `MAINTENANCE`
- **BookingStatus** (state machine): `CONFIRMED → CHECKED_IN → CHECKED_OUT`; any state `→ CANCELLED`

## Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to HTTP status and returns `ErrorResponse` JSON:

```json
{
  "timestamp": "2026-06-26T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Room 201 is already booked for 2026-07-10 to 2026-07-13",
  "path": "/api/bookings"
}
```

| Exception | HTTP |
|-----------|------|
| `ResourceNotFoundException` | 404 |
| `BookingConflictException` | 409 |
| `MethodArgumentNotValidException` | 400 (with field-level messages) |
| `IllegalArgumentException` / bad dates | 400 |

## Repository overlap query

`BookingRepository` exposes:
```java
List<Booking> findByRoomIdAndStatusNot(Long roomId, BookingStatus status);
```
`BookingService` calls this with `CANCELLED` to retrieve all active bookings for a room, then applies the overlap predicate in Java:
```
existing.checkInDate < new.checkOutDate  AND  new.checkInDate < existing.checkOutDate
```

## Seed data (DataSeeder on startup)

| roomNumber | type | pricePerNight (RM) | status |
|------------|------|--------------------|--------|
| 101 | SINGLE | 150 | AVAILABLE |
| 102 | DOUBLE | 250 | AVAILABLE |
| 201 | SUITE | 450 | AVAILABLE |
| 202 | DELUXE | 600 | AVAILABLE |
| 301 | SINGLE | 150 | MAINTENANCE |

Plus 2 seed bookings on rooms 101 and 102 so the availability search and overlap demos work immediately.

## Stack

- Java 17, Spring Boot 4.x, Maven
- Spring Web (MVC), Spring Data JPA, Bean Validation (jakarta.validation)
- H2 in-memory database (zero-config, reproducible — no external DB needed)
- `@RestControllerAdvice` global exception handler for consistent JSON error shape
