# Hotel Room Booking System — Build Guide
### How to develop the project in VS Code with Claude Code

This is the hands-on companion to `hotel-booking-design-blueprint.md`. Keep both files in a `docs/` folder inside your project so Claude Code can read them with `@`-mentions.

Work through the parts in order. In Part D, paste each phase's prompt into Claude Code, then run its "Verify before continuing" check before moving to the next phase.

---

## Part A — Install your tools (once per machine)

1. **JDK 21 (LTS)** — install Eclipse Temurin 21 from adoptium.net. (Java 17 also works; Spring Boot 3.x needs 17+.)
2. **VS Code** — from code.visualstudio.com.
3. **Extensions** — open the Extensions view (`Ctrl+Shift+X`) and install:
   - *Extension Pack for Java* (Microsoft) — language support, debugger, test runner, Maven.
   - *Spring Boot Extension Pack* (Broadcom/VMware) — Spring tools + dashboard.
   - *Claude Code* (Anthropic) — confirm the publisher is Anthropic.
4. **Sign in to Claude Code** — click the Spark icon in the sidebar; it opens a browser to authorize with your Claude account (any paid subscription or a Console account; no API key needed). The extension bundles its own CLI, so you do not need Node.js for the graphical panel.

---

## Part B — Generate the Spring Boot project

Use Spring Initializr at start.spring.io with these settings:

| Setting | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | latest 3.x (default) |
| Group | com.example |
| Artifact | hotelbooking |
| Name | hotelbooking |
| Package name | com.example.hotelbooking |
| Packaging | Jar |
| Java | 21 |

**Dependencies (add all four):** Spring Web · Spring Data JPA · H2 Database · Validation

Click **Generate**, unzip the download, and open the unzipped folder in VS Code.

> Alternative: the Spring Boot Extension Pack can do this from inside VS Code — Command Palette (`Ctrl+Shift+P`) → "Spring Initializr: Generate a Maven Project".

---

## Part C — Wire in the design docs

1. Create a `docs/` folder in the project root.
2. Put `hotel-booking-design-blueprint.md` and this guide inside it.
3. Open Claude Code (Spark icon) and run `/init` so it generates a `CLAUDE.md` describing your project.
4. Switch Claude Code to **Plan mode** for the multi-file phases below.

---

## Part D — Build it phase by phase

Paste each prompt into Claude Code. Build, verify, then continue.

### Phase 0 — Orientation
```
Read @docs/hotel-booking-design-blueprint.md — it is the full spec for this project.
Then update CLAUDE.md to capture the architecture: a layered Spring Boot app
(Controller -> Service -> Repository), DTOs separating the API from JPA entities,
a global exception handler, and an in-memory H2 database. Do not write feature code yet.
```
**Verify:** `CLAUDE.md` reflects the layers and package plan from section 4 of the blueprint.

### Phase 1 — Domain model
```
Following section 3 of @docs/hotel-booking-design-blueprint.md, create JPA entities
Room and Booking and the enums RoomType, RoomStatus, BookingStatus in a `model` package.
Room has @OneToMany to Booking; Booking has @ManyToOne to Room. Add Bean Validation
annotations (@NotBlank, @Email, @NotNull, @FutureOrPresent) where the spec calls for them.
Use plain Java classes with a no-arg constructor and getters/setters.
```
**Verify:** the project still compiles (`./mvnw compile`); both entities and three enums exist.

### Phase 2 — Repositories
```
Create RoomRepository and BookingRepository extending JpaRepository.
In BookingRepository add a query method that finds a given room's bookings whose status
is NOT CANCELLED and whose date range overlaps a supplied checkIn/checkOut — this powers
double-booking detection. Add whatever query RoomService needs for the available-rooms search.
```
**Verify:** repositories compile; the overlap query method is present.

### Phase 3 — DTOs
```
Create the DTOs from section 5 of the blueprint as Java records: BookingRequest (incoming,
with validation annotations), BookingResponse, RoomResponse, and ErrorResponse.
Controllers should accept and return these records, never the JPA entities directly.
```
**Verify:** four record types exist in a `dto` package.

### Phase 4 — Services (the business logic)
```
Implement RoomService and BookingService per sections 4 and 6 of the blueprint.
createBooking must: confirm the room exists (else throw ResourceNotFoundException),
reject rooms in MAINTENANCE, reject invalid date ranges, detect overlapping non-cancelled
bookings (throw BookingConflictException), compute totalPrice = nights * pricePerNight
on the server, then save and return a BookingResponse.
RoomService.searchAvailable must return rooms that have no overlapping non-cancelled booking
in the requested range, optionally filtered by RoomType.
Add clear comments on the overlap algorithm.
```
**Verify:** services compile; read the overlap comment and make sure you can explain it.

### Phase 5 — Exceptions
```
Create ResourceNotFoundException and BookingConflictException, plus a GlobalExceptionHandler
(@RestControllerAdvice) that maps ResourceNotFoundException -> 404, BookingConflictException
-> 409, validation errors (MethodArgumentNotValidException) -> 400 with field messages, and
bad arguments -> 400. Every error returns the ErrorResponse JSON shape from section 7.
```
**Verify:** handler compiles and covers all four mappings.

### Phase 6 — Controllers
```
Create RoomController and BookingController exposing the endpoints in section 5 of the
blueprint with correct HTTP methods, URLs, and status codes (201 Created on POST,
204 No Content on DELETE). Use @Valid on request bodies and return JSON via the DTOs.
```
**Verify:** all endpoints from section 5 exist with the right `@GetMapping`/`@PostMapping`/etc.

### Phase 7 — Config + seed data
```
Add src/main/resources/application.properties for an in-memory H2 database with the H2
console enabled (see the snippet in the build guide). Then create a DataSeeder
(CommandLineRunner) that inserts the five sample rooms and two seed bookings from section 9.
```
Use this `application.properties`:
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
**Verify:** see Part E.

---

## Part E — Run and smoke-test

1. Run: `./mvnw spring-boot:run` (or use the Spring Boot Dashboard / run the main class).
2. Browser: open `http://localhost:8080/api/rooms` — you should see the five seeded rooms as JSON.
3. H2 console: open `http://localhost:8080/h2-console`, set JDBC URL to `jdbc:h2:mem:hoteldb`, user `sa`, no password — confirm the ROOM and BOOKING tables hold the seed data.

If anything fails, paste the error into Claude Code and ask it to diagnose and fix.

---

## Part F — Postman testing (required by the assignment)

Build one collection and run the requests from section 10 of the blueprint, in order. The four edge cases are the ones that earn marks:

- POST a booking on a room with overlapping dates → expect **409 Conflict**
- POST with checkOut before checkIn → expect **400 Bad Request**
- POST with a non-existent roomId → expect **404 Not Found**
- GET `/api/rooms/available` after booking → the booked room should disappear for those dates

Export the collection as `.json` and commit it to the repo (e.g. `postman/hotel-booking.postman_collection.json`).

---

## Part G — Frontend (served from Spring Boot)

Paste into Claude Code once the API works:
```
In this Spring Boot project, create a static frontend in src/main/resources/static
(index.html, styles.css, app.js — vanilla JS, no framework, no build step). Three tabs:
Rooms, Search & book, and Bookings. "Search & book" has check-in/check-out date inputs and
a room-type dropdown that call GET /api/rooms/available and render results as cards with a
Book button; Book opens a form posting to POST /api/bookings, shows the returned total price,
and handles 400/404/409 with a clear message. "Rooms" lists GET /api/rooms as cards with a
status badge. "Bookings" lists GET /api/bookings with Edit (PUT) and Cancel (DELETE) actions.
Use plain fetch and clean flat styling with cards and subtle borders.
```
**Verify:** open `http://localhost:8080/` — all five functionalities are reachable from the UI.

---

## Part H — README, comments, and submission

1. **README** — have Claude Code draft it from section 11 of the blueprint, then edit it yourselves so it sounds like you. Include setup steps, the API table, business rules in plain English, team contributions, and a link to the Postman collection.
2. **Comments** — make sure the overlap algorithm and any non-obvious code are commented (rubric #7).
3. **Diagrams** — add the ER diagram and an architecture diagram to the README (rubric #1, #10).
4. **GitHub** — push to a **public** repo (a private repo counts as non-submission).
5. **Video** — record per section 13 of the blueprint: `.mp4`, under 20 minutes, under 200 MB, all members on camera.
6. **Submit** — GitHub link + video via the E-Learning portal before the deadline.

---

## Working-with-Claude-Code tips

- **Plan mode** for any multi-file phase — review the plan before approving.
- **One phase at a time** — verify each before the next; small steps are easier to debug and to understand.
- **Ask "why"** — after each phase, ask Claude Code to explain its choices. You must be able to defend the code in your video and viva, and that understanding is what the integrity and systems-thinking criteria reward.
- **Paste errors back** — when a build or test fails, give Claude Code the exact error text.
- **Disclose AI use** — follow your course's policy on disclosing AI assistance; keep the code something every member genuinely understands.
- **Commit per phase** — small, frequent commits from each member make the equal-participation evidence (rubric #8, #9) visible in the history.
