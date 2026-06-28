# Hotel Room Booking System

> SECJ4383 Software Construction · Assignment 2 — REST API with Spring Boot

A backend system for managing hotel rooms and guest bookings, built with Spring Boot
using a clean layered architecture (**Controller → Service → Repository**). It exposes
five functionalities as RESTful JSON endpoints and ships with a warm boutique-lodge web
UI ("Cedar & Clay") served straight from the app.

The defining business rule — **a room cannot be double-booked for overlapping dates** —
is what lifts this above a flat CRUD app.

---

## The five functionalities

As required by the assignment, the system implements **five distinct functionalities**,
each backed by the full layered architecture (class → service → repository) and exposed
as a RESTful web service:

| # | Functionality | What it does | Endpoint |
|---|---------------|--------------|----------|
| 1 | **Create a booking** | Reserve a room for a date range — validates the room, dates, and availability, then computes the price server-side | `POST /api/bookings` ★ |
| 2 | **Retrieve bookings** | List all bookings, or fetch a single booking by its ID | `GET /api/bookings`, `GET /api/bookings/{id}` |
| 3 | **Update a booking** | Change a booking's guest details or dates, re-checking availability | `PUT /api/bookings/{id}` |
| 4 | **Cancel / delete a booking** | Remove a booking, freeing the room's dates again | `DELETE /api/bookings/{id}` |
| 5 | **Search available rooms** | Find rooms that are free for a given check-in/check-out range, optionally filtered by room type | `GET /api/rooms/available` ★ |

★ = the two functionalities showcased as the required REST web-services
(**Create booking** and **Search available rooms**). All five are fully exposed as REST
endpoints returning JSON.

> Supporting room management (`GET/POST/PUT/DELETE /api/rooms`) is also provided so the
> five booking/search functionalities have data to operate on.

---

## Tech stack

| Choice | Why |
|--------|-----|
| **Java 17**, **Spring Boot 4.x** | LTS Java, auto-configuration, embedded server |
| **Maven** | Single `pom.xml` dependency management |
| **Spring Web (MVC)** | REST controllers |
| **Spring Data JPA** | ORM, no hand-written SQL |
| **H2 (in-memory)** | Zero-config — clone and run instantly, fully reproducible |
| **Bean Validation** (`jakarta.validation`) | Declarative input validation at the boundary |
| **`@RestControllerAdvice`** | One consistent JSON error shape across all endpoints |
| **Vanilla HTML/CSS/JS** | Static frontend, no build step |

---

## Architecture

A strict four-layer structure under `com.example.hotelbooking`:

```
controller/   HTTP only — delegates to services, returns DTOs
service/      business logic — overlap detection, pricing, validation
repository/   Spring Data JPA — custom overlap query lives here
model/        JPA entities (Room, Booking) + enums (RoomType, RoomStatus, BookingStatus)
dto/          request/response records — decouple the API from JPA entities
exception/    custom exceptions + GlobalExceptionHandler
config/        DataSeeder — seeds sample data on startup
```

- **Controllers never expose JPA entities** — all input/output goes through DTO records.
- **`totalPrice` is always computed server-side** (`nights × pricePerNight`); never accepted from the client.
- The **`Room` (1) ──< `Booking` (many)** relationship (`@OneToMany` / `@ManyToOne`) is the core of the data model.

---

## Prerequisites

- **JDK 17 or newer** (`java -version` to check)
- No Maven install needed — the project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`)
- An internet connection on first run (Maven downloads dependencies; the web UI loads fonts/imagery)

---

## Run it

```bash
# 1. Clone
git clone <your-repo-url>
cd hotelbooking

# 2. Run (macOS/Linux)
./mvnw spring-boot:run

#    Run (Windows)
mvnw.cmd spring-boot:run
```

The app starts on **http://localhost:8080**.

| URL | What |
|-----|------|
| http://localhost:8080/ | The Cedar & Clay booking web UI |
| http://localhost:8080/api/rooms | REST API (JSON) |
| http://localhost:8080/h2-console | H2 database console |

**H2 console login:** JDBC URL `jdbc:h2:mem:hoteldb`, user `sa`, no password.

> The database is in-memory, so it resets on every restart and re-seeds 5 rooms + 2 bookings.

---

## API reference

### Rooms
| Method | URL | Purpose | Success | Errors |
|--------|-----|---------|---------|--------|
| GET | `/api/rooms` | List all rooms | 200 | — |
| GET | `/api/rooms/{id}` | Get one room | 200 | 404 |
| GET | `/api/rooms/available?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&type=SUITE` | Search available rooms | 200 | 400 |
| POST | `/api/rooms` | Add a room | 201 | 400 |
| PUT | `/api/rooms/{id}` | Update a room | 200 | 404, 400 |
| DELETE | `/api/rooms/{id}` | Remove a room | 204 | 404 |

### Bookings
| Method | URL | Purpose | Success | Errors |
|--------|-----|---------|---------|--------|
| POST | `/api/bookings` | Create a booking | 201 | 400, 404, 409 |
| GET | `/api/bookings` | List all bookings | 200 | — |
| GET | `/api/bookings/{id}` | Get one booking | 200 | 404 |
| PUT | `/api/bookings/{id}` | Update a booking | 200 | 400, 404, 409 |
| PATCH | `/api/bookings/{id}/check-in` | Check the guest in (CONFIRMED → CHECKED_IN) | 200 | 404, 409 |
| PATCH | `/api/bookings/{id}/check-out` | Check the guest out (CHECKED_IN → CHECKED_OUT) | 200 | 404, 409 |
| PATCH | `/api/bookings/{id}/cancel` | Soft-cancel a booking (→ CANCELLED), freeing its dates | 200 | 404, 409 |
| DELETE | `/api/bookings/{id}` | Permanently delete a booking | 204 | 404 |

### Example — create a booking

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 3,
    "guestName": "Aisha Rahman",
    "guestEmail": "aisha@example.com",
    "checkInDate": "2026-07-10",
    "checkOutDate": "2026-07-13"
  }'
```

```json
{
  "id": 3,
  "roomNumber": "201",
  "roomType": "SUITE",
  "guestName": "Aisha Rahman",
  "guestEmail": "aisha@example.com",
  "checkInDate": "2026-07-10",
  "checkOutDate": "2026-07-13",
  "nights": 3,
  "totalPrice": 1350.00,
  "status": "CONFIRMED"
}
```

---

## Booking lifecycle

A booking moves through a small state machine, driven by three `PATCH` endpoints:

```
CONFIRMED ──check-in──▶ CHECKED_IN ──check-out──▶ CHECKED_OUT
    │                        │
    └────────cancel──────────┴──▶ CANCELLED
```

- `PATCH /api/bookings/{id}/check-in` — only a `CONFIRMED` booking may be checked in.
- `PATCH /api/bookings/{id}/check-out` — only a `CHECKED_IN` booking may be checked out.
- `PATCH /api/bookings/{id}/cancel` — **soft cancel.** The record is kept but set to
  `CANCELLED`, which the overlap check excludes — so the room's dates become bookable
  again (business rule 5). A `CHECKED_OUT` or already-`CANCELLED` booking cannot be cancelled.

Illegal transitions return **409 Conflict** with a clear message (e.g. checking out a
booking that was never checked in). `DELETE`, by contrast, permanently removes the record;
prefer `/cancel` when you want to free the dates while keeping the history.

```bash
curl -X PATCH http://localhost:8080/api/bookings/1/check-in
```

---

## Booking Summary

`GET /api/bookings/summary` returns a lightweight booking overview with:

- total bookings
- confirmed, checked-in, checked-out, and cancelled counts
- upcoming bookings
- total revenue

---

## Business rules

1. **No double-booking (409 Conflict).** Two ranges overlap when
   `existing.checkIn < new.checkOut AND new.checkIn < existing.checkOut`. The check runs
   in `BookingService` against all non-cancelled bookings for the room.
2. **Valid dates (400 Bad Request).** `checkOutDate` must be strictly after `checkInDate`,
   and `checkInDate` cannot be in the past.
3. **Room must exist and be bookable.** Booking a missing room → 404; a room under
   `MAINTENANCE` → rejected with a clear message.
4. **Server-side pricing.** `totalPrice = nights × pricePerNight`, always computed on the server.
5. **Cancellation frees the dates.** A `CANCELLED` booking is excluded from the overlap
   check, so the room becomes available again.

### Error shape

Every error returns the same JSON body from `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-06-26T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Room is already booked for 2026-07-10 to 2026-07-13",
  "path": "/api/bookings"
}
```

| Exception | HTTP status |
|-----------|-------------|
| `ResourceNotFoundException` | 404 Not Found |
| `BookingConflictException` | 409 Conflict |
| validation failure | 400 Bad Request (with field messages) |
| invalid date range / bad argument | 400 Bad Request |

---

## Seed data

| Room | Type | Price/night (RM) | Status |
|------|------|------------------|--------|
| 101 | SINGLE | 150 | AVAILABLE |
| 102 | DOUBLE | 250 | AVAILABLE |
| 201 | SUITE | 450 | AVAILABLE |
| 202 | DELUXE | 600 | AVAILABLE |
| 301 | SINGLE | 150 | MAINTENANCE |

Plus two seed bookings (on rooms 101 and 102) so the availability search and overlap
demos return meaningful results immediately.

---

## Tests

```bash
./mvnw test
```

---

## Project structure

```
src/main/java/com/example/hotelbooking/
├── HotelbookingApplication.java
├── controller/   RoomController, BookingController
├── service/      RoomService, BookingService
├── repository/   RoomRepository, BookingRepository
├── model/        Room, Booking, RoomType, RoomStatus, BookingStatus
├── dto/          BookingRequest, BookingResponse, RoomRequest, RoomResponse, ErrorResponse
├── exception/    ResourceNotFoundException, BookingConflictException, GlobalExceptionHandler
└── config/       DataSeeder
src/main/resources/
├── application.properties
└── static/       index.html, styles.css, app.js  (the web UI)
docs/             design blueprint + build guide
```

---

## Team

| Member | Responsibilities |
|--------|------------------|
| **Lim Bo Yuan** | Project setup & configuration (`pom.xml`, `application.properties`), domain model (`Room`, `Booking` entities + enums), repositories, and the `DataSeeder` |
| **Loh Chee Huan** | Booking functionalities — `BookingService` (double-booking detection + server-side pricing) and `BookingController`: create, retrieve, update, and cancel bookings (functionalities 1–4) |
| **Khoo Teong Lee** | Room & search functionality — `RoomService`, `RoomController`, the available-rooms search (functionality 5), and the `GlobalExceptionHandler` error handling |
| **Lim Yu An** | Frontend web UI (Cedar & Clay), Postman test collection (happy paths + edge cases), and project documentation (README) |
