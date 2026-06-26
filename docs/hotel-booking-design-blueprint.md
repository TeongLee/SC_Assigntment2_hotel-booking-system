# Hotel Room Booking System — Design Blueprint
### SECJ4383 Software Construction · Assignment 2 (REST API · Spring Boot)

This document is the single source of truth for the project. It is the brief you take into **Claude Design** (to produce the diagrams) and the spec you hand to **Claude Code** (to build the app). Every section is mapped to the rubric so the team knows *why* each decision exists.

---

## 1. Project summary

A backend system for managing hotel rooms and guest bookings. Built with Spring Boot using a clean layered architecture (Controller → Service → Repository), it implements **five functionalities** and exposes them as RESTful endpoints returning JSON, tested in Postman.

The defining business rule — **a room cannot be double-booked for overlapping dates** — is what lifts this above a flat CRUD app and unlocks the "Highly Developed" descriptors for requirement analysis, technical justification, REST design, and systems thinking.

---

## 2. Requirement → feature mapping (Rubric #1)

| Assignment requirement | How this project satisfies it |
|---|---|
| Choose one application | Hotel Room Booking System |
| Implement 5 distinct functionalities | Create / Retrieve / Update / Delete booking + Search available rooms |
| Each with class + service + repository | Full layered architecture for both `Room` and `Booking` |
| Expose 2 as REST web-services | `POST /api/bookings` and `GET /api/rooms/available` are the two showcase endpoints (all five are actually exposed as REST) |
| Proper RESTful URLs | `/api/rooms`, `/api/rooms/{id}`, `/api/bookings`, `/api/bookings/{id}` |
| Correct HTTP methods | GET, POST, PUT, DELETE used semantically |
| JSON responses | All responses serialized to JSON via Jackson |
| Tested in Postman | Full collection with happy paths + edge cases |

---

## 3. Data model

### Entity: Room
| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| roomNumber | String | unique, not blank (e.g. "101") |
| type | RoomType (enum) | SINGLE, DOUBLE, SUITE, DELUXE |
| pricePerNight | BigDecimal | positive |
| status | RoomStatus (enum) | AVAILABLE, MAINTENANCE |

### Entity: Booking
| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, auto-generated |
| room | Room (ManyToOne, FK room_id) | must exist |
| guestName | String | not blank |
| guestEmail | String | valid email format |
| checkInDate | LocalDate | not null, today or later |
| checkOutDate | LocalDate | not null, strictly after checkInDate |
| status | BookingStatus (enum) | CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED |
| totalPrice | BigDecimal | computed server-side, read-only to client |

### Relationship
One `Room` has many `Booking` records (`@OneToMany` / `@ManyToOne`). This is the relationship that demonstrates systems thinking (Rubric #10).

### Enums (the "state machine" talking points)
- **RoomType**: SINGLE, DOUBLE, SUITE, DELUXE
- **RoomStatus**: AVAILABLE, MAINTENANCE
- **BookingStatus**: CONFIRMED → CHECKED_IN → CHECKED_OUT, or → CANCELLED at any point

---

## 4. Layered architecture (Rubric #1, #10)

```
com.example.hotelbooking
├── HotelBookingApplication.java        (main / @SpringBootApplication)
├── controller
│   ├── RoomController.java
│   └── BookingController.java
├── service
│   ├── RoomService.java
│   └── BookingService.java             (holds overlap + price logic)
├── repository
│   ├── RoomRepository.java             (extends JpaRepository)
│   └── BookingRepository.java
├── model
│   ├── Room.java
│   ├── Booking.java
│   ├── RoomType.java
│   ├── RoomStatus.java
│   └── BookingStatus.java
├── dto
│   ├── BookingRequest.java             (incoming, validated with @Valid)
│   ├── BookingResponse.java            (outgoing, hides JPA internals)
│   ├── RoomResponse.java
│   └── ErrorResponse.java
├── exception
│   ├── ResourceNotFoundException.java
│   ├── BookingConflictException.java
│   └── GlobalExceptionHandler.java     (@RestControllerAdvice)
└── config
    └── DataSeeder.java                 (CommandLineRunner — seeds sample data)
```

**Why this structure (justification ammo for Rubric #2):**
- **Controller / Service / Repository separation** keeps each layer single-responsibility: controllers handle HTTP, services hold business rules, repositories handle persistence.
- **DTOs** decouple the public API contract from the database entities, so internal schema changes don't break clients.
- **Global exception handler** produces a consistent JSON error shape across every endpoint instead of leaking stack traces.
- **DataSeeder** makes the demo reproducible — the grader gets meaningful data the moment the app starts.

---

## 5. REST API contract (Rubric #4)

> The two **showcase endpoints** required by the assignment are marked ★. All five functionalities are exposed as REST for completeness.

### Rooms
| Method | URL | Purpose | Success | Errors |
|---|---|---|---|---|
| GET | `/api/rooms` | List all rooms | 200 | — |
| GET | `/api/rooms/{id}` | Get one room | 200 | 404 |
| ★ GET | `/api/rooms/available?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD&type=SUITE` | **Search available rooms** (functionality #5) | 200 | 400 (bad dates) |
| POST | `/api/rooms` | Add a room | 201 | 400 |
| PUT | `/api/rooms/{id}` | Update a room (e.g. set MAINTENANCE) | 200 | 404, 400 |
| DELETE | `/api/rooms/{id}` | Remove a room | 204 | 404 |

### Bookings
| Method | URL | Purpose | Success | Errors |
|---|---|---|---|---|
| ★ POST | `/api/bookings` | **Create booking** (functionality #1) | 201 | 400, 404, 409 |
| GET | `/api/bookings` | List all bookings (functionality #2) | 200 | — |
| GET | `/api/bookings/{id}` | Get one booking | 200 | 404 |
| PUT | `/api/bookings/{id}` | Update booking (functionality #3) | 200 | 400, 404, 409 |
| DELETE | `/api/bookings/{id}` | Cancel/delete booking (functionality #4) | 204 | 404 |

### Sample request — `POST /api/bookings`
```json
{
  "roomId": 3,
  "guestName": "Aisha Rahman",
  "guestEmail": "aisha@example.com",
  "checkInDate": "2026-07-10",
  "checkOutDate": "2026-07-13"
}
```

### Sample response — `201 Created`
```json
{
  "id": 7,
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

## 6. Business rules (the differentiators)

### Rule 1 — No overlapping bookings (returns 409 Conflict)
Two date ranges overlap when:
```
existing.checkInDate < new.checkOutDate
AND
new.checkInDate < existing.checkOutDate
```
The check runs in `BookingService` against all **non-cancelled** bookings for that room (use a repository query like `findByRoomIdAndStatusNot(roomId, CANCELLED)`). On conflict, throw `BookingConflictException` → handled as `409`.

### Rule 2 — Valid date range (returns 400 Bad Request)
`checkOutDate` must be strictly after `checkInDate`; `checkInDate` cannot be in the past. Enforced with Bean Validation annotations plus a service-level guard.

### Rule 3 — Room must exist and be bookable (returns 404 / 409)
Booking a non-existent room → `404`. Booking a room in `MAINTENANCE` → rejected with a clear message.

### Rule 4 — Server-side price calculation
`totalPrice = ChronoUnit.DAYS.between(checkIn, checkOut) * room.pricePerNight`. Never accept a price from the client.

### Rule 5 — Cancellation frees the dates
A `CANCELLED` booking is excluded from the overlap check, so the room becomes available again.

---

## 7. Error handling design (Rubric #4)

Consistent JSON error body from `GlobalExceptionHandler`:
```json
{
  "timestamp": "2026-06-26T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Room 201 is already booked for 2026-07-10 to 2026-07-13",
  "path": "/api/bookings"
}
```

| Exception | HTTP status |
|---|---|
| ResourceNotFoundException | 404 Not Found |
| BookingConflictException | 409 Conflict |
| MethodArgumentNotValidException (validation) | 400 Bad Request (with field errors) |
| IllegalArgumentException / invalid dates | 400 Bad Request |

---

## 8. Technology stack & justifications (Rubric #2)

| Choice | Why | Alternative considered |
|---|---|---|
| Spring Boot 3.x, Java 17 (LTS) | Auto-configuration, embedded server, industry standard | Plain Servlets (too much boilerplate) |
| Maven | Familiar dependency management, single `pom.xml` | Gradle (equally valid; team familiarity decides) |
| Spring Web | REST controller support | — |
| Spring Data JPA | ORM, removes hand-written SQL, portable | JDBC template (more boilerplate) |
| H2 in-memory DB | **Zero-config** — grader clones and runs instantly, fully reproducible demo | MySQL/PostgreSQL (more "production" but adds setup friction for marking) |
| Bean Validation (jakarta.validation) | Declarative input validation at the boundary | Manual `if` checks (scatters logic) |
| @RestControllerAdvice | One place for consistent error responses | Try/catch in every controller (repetitive) |

> Being able to name the alternative you rejected and *why* is exactly what the "Highly Developed" descriptor for Rubric #2 asks for.

---

## 9. Sample data (DataSeeder)

| roomNumber | type | pricePerNight (RM) | status |
|---|---|---|---|
| 101 | SINGLE | 150 | AVAILABLE |
| 102 | DOUBLE | 250 | AVAILABLE |
| 201 | SUITE | 450 | AVAILABLE |
| 202 | DELUXE | 600 | AVAILABLE |
| 301 | SINGLE | 150 | MAINTENANCE |

Plus two seed bookings on rooms 101 and 102 so the availability search and overlap demos return meaningful results immediately.

---

## 10. Postman test plan (Rubric #5)

Build one collection with these requests. The edge cases are what earn the "considers edge cases" descriptor.

| # | Request | Expected | Purpose |
|---|---|---|---|
| 1 | GET `/api/rooms` | 200 + 5 rooms | Confirm seed data |
| 2 | GET `/api/rooms/available?checkIn=2026-07-10&checkOut=2026-07-13` | 200 + available list | Showcase endpoint #2 |
| 3 | POST `/api/bookings` (valid) | 201 + computed totalPrice | Showcase endpoint #1 — happy path |
| 4 | POST `/api/bookings` (same room, overlapping dates) | **409 Conflict** | ★ Edge case: double-booking |
| 5 | POST `/api/bookings` (checkOut before checkIn) | **400 Bad Request** | ★ Edge case: bad dates |
| 6 | POST `/api/bookings` (roomId that doesn't exist) | **404 Not Found** | ★ Edge case: missing room |
| 7 | GET `/api/bookings/{id}` | 200 | Retrieve by ID |
| 8 | PUT `/api/bookings/{id}` (new dates) | 200 | Update + re-validate |
| 9 | DELETE `/api/bookings/{id}` | 204 | Cancel/delete |
| 10 | GET `/api/rooms/available` (after booking) | 200, booked room absent | Prove the rule works end-to-end |

Export the collection (`.json`) and commit it to the repo.

---

## 11. README outline (Rubric #7)

1. Project title + one-paragraph description
2. Tech stack
3. Architecture overview (embed the architecture diagram) + the layer explanation
4. ER diagram (embed it)
5. Prerequisites (Java 17, Maven)
6. Setup & run: `git clone …`, `mvn spring-boot:run`
7. H2 console access (`/h2-console`) + JDBC URL
8. Full API table (copy section 5)
9. Sample requests (curl or Postman screenshots)
10. Business rules explained (copy section 6 in plain English)
11. Team members + each person's contributions
12. Link to the Postman collection in the repo

Also: comment the non-obvious code (especially the overlap algorithm) — the rubric explicitly rewards thorough comments.

---

## 12. Suggested team task division (Rubric #8, #9)

> Adjust to your group size. The goal is balanced ownership that's visible in both the commit history and the video.

| Member | Owns | Demos in video |
|---|---|---|
| A | Project setup, `pom.xml`, entities + enums, repositories, DataSeeder | Spring Boot setup walkthrough |
| B | `BookingService` (overlap + price logic), `BookingController` | Booking endpoints + the 409 edge case |
| C | `RoomService`, `RoomController`, availability search, exception handler | Room search + error handling |
| D | Postman collection, full testing, README + documentation, video editing | Postman demo + project overview |

Use a shared GitHub repo with each member committing their own work — the commit history is the evidence of equal participation.

---

## 13. Video script outline — under 20 minutes (Rubric #6)

| Time | Segment | Who |
|---|---|---|
| 0:00–2:00 | Intro: team, app overview, requirement → feature mapping | All members on camera |
| 2:00–5:00 | Spring Boot setup: project structure, `pom.xml` dependencies, `application.properties`, the layers | Member A |
| 5:00–9:00 | Code walkthrough: entities + relationship, the overlap + price logic, exception handling (the systems-thinking story) | Members B & C |
| 9:00–14:00 | Postman demo of both showcase endpoints + the 409 / 400 / 404 edge cases | Member D |
| 14:00–18:00 | Live demo: all five functionalities end-to-end | Rotate members |
| 18:00–19:30 | Wrap: design justifications, what each member built | All members |

Keep it `.mp4` and under 200 MB.

---

## 14. Diagrams to create in Claude Design

Take these three into Claude Design:
1. **ER diagram** — Room (1) ─< Booking (many), with the fields from section 3. (A reference version is already rendered in the chat.)
2. **Layered architecture diagram** — four horizontal layers (Controller → Service → Repository → Database) with the class names from section 4, plus arrows showing a request flowing down and a response flowing up.
3. **REST API contract sheet** — a clean visual table of the endpoints from section 5 (method, URL, purpose, status codes).

These diagrams go straight into the README and the video slides, and they *are* your evidence for Rubric #1 and #10.

---

## 15. Rubric coverage checklist

| # | Criterion | Covered by |
|---|---|---|
| 1 | Requirement Analysis & System Design | §2, §4, §14 diagrams |
| 2 | Justification of Technical Choices | §8 (with rejected alternatives) |
| 3 | Implementation of Five Functionalities | §5 (all 5 as REST, fully integrated) |
| 4 | REST API Design & Architecture | §5, §6, §7 (methods, URLs, JSON, error handling) |
| 5 | Testing & Validation via Postman | §10 (happy paths + edge cases) |
| 6 | Video Demonstration | §13 script |
| 7 | Documentation & README | §11 + code comments |
| 8 | Team Collaboration & Task Division | §12 |
| 9 | Equal Participation | §12 + commit history + all on camera |
| 10 | Academic Integrity & Systems Thinking | original work, §3 relationship, §4 layer interaction, §6 rules |
