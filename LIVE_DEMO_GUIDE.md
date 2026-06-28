# Live Demo Guide — Member D (Lim Yu An)

Your segment of the video (≈ 14:00–18:00). You'll **share your screen with the web app open
in a browser** and demonstrate **all five functionalities** end-to-end through our
"Cedar & Clay" interface.

This guide tells you exactly what to click and what to say.

---

## Part 1 — Set up (before recording)

1. **Start the app.** In a terminal in the project folder:
   ```bash
   mvnw spring-boot:run
   ```
   Wait for **"Started HotelbookingApplication"**.
2. **Open the web app** in your browser: <http://localhost:8080>
3. **Restart the app right before recording** (stop with `Ctrl+C`, run it again) so the
   database is fresh — 5 rooms, 2 existing bookings. This keeps the demo clean.
4. Maximise the browser window and zoom to ~110% so everything is readable on video.

### The interface at a glance
- Three tabs at the top: **Stay** (search & book) · **Rooms** (all rooms) · **Reservations** (manage bookings)
- The home page has a search panel: **Arrive**, **Depart**, and **Room style**, with a
  **Find a room** button.

---

## Part 2 — The five functionalities, in demo order

| Step | Functionality | Where | Action |
|------|---------------|-------|--------|
| 1 | **#5 Search available rooms** | Stay tab | Pick dates → Find a room |
| 2 | **#1 Create a booking** | Stay tab | Book this room → fill form → Confirm |
| 3 | **#2 Retrieve bookings** | Reservations + Rooms tabs | View the booking and room list |
| 4 | **#3 Update a booking** | Reservations tab | Edit → change dates → Save |
| 5 | **#4 Cancel a booking** | Reservations tab | Cancel → confirm |
| ★ | Bonus: double-booking blocked | Stay tab | Try to re-book the same room/dates |

---

## Part 3 — Script (what to do + say)

### Intro (15s)
> "Thanks Teong Lee. I'll now demonstrate all five functionalities live, through the web
> interface we built, which talks to the same REST API."

### Step 1 — Search available rooms (functionality #5)
*On the Stay tab, pick an Arrive date and a Depart date a few days apart. Leave Room style as
"Any style" (or pick Suite). Click **Find a room**.*
> "First, searching for available rooms. I'll choose my check-in and check-out dates and
> search. The app calls our `/api/rooms/available` endpoint and shows the rooms that are free
> for those dates as cards."

### Step 2 — Create a booking (functionality #1)
*Click **Book this room** on one of the cards (e.g. the Suite). The booking form opens. Fill in
a guest name and email; the dates are pre-filled. Click **Confirm booking**.*
> "I'll book this suite. I enter the guest's name and email — the dates carry over from my
> search — and confirm. Notice the confirmation shows the **total price, calculated on the
> server**, and the booking is created."

### Step 3 — Retrieve bookings (functionality #2)
*The app jumps to the Reservations tab. Point at the new booking card.*
> "We're taken to the Reservations tab, which lists all bookings from the `/api/bookings`
> endpoint — here's the one I just made, with the guest details, dates, and total."

*Optionally click the **Rooms** tab.*
> "The Rooms tab also lists every room with its status — available or under maintenance."

### Step 4 — Update a booking (functionality #3)
*Back on Reservations, click **Edit** on your booking. Change the dates (or guest name) in the
form. Click **Save changes**.*
> "Bookings can be updated. I'll edit this one and change the dates. The system re-validates
> and re-checks availability before saving, then recalculates the price."

### Step 5 — Cancel a booking (functionality #4)
*Click **Cancel** on the booking. Confirm the pop-up. The reservation stays visible with a
`CANCELLED` status, which shows that the app keeps the history while freeing the dates.*
> "And bookings can be cancelled. Once I confirm, the status changes to CANCELLED. Because of
> our overlap rule, those dates are now free for that room again while the booking history is
> still kept."

### Bonus — Double-booking prevented (optional, strong finish)
*Go to Stay, search dates that overlap one of the seed bookings (rooms 101 or 102 already have
bookings), book that room, then try to book it again for the same dates.*
> "Finally, the rule that matters most: if I try to book a room that's already taken for those
> dates, the system blocks it with a clear error message. No double-booking."

### Hand-off (5s)
> "That's all five functionalities working end-to-end. Back to the team to wrap up."

---

## Part 4 — Tips

- **Move the mouse deliberately** and pause on each result so viewers can follow.
- After creating a booking, **point out the total price** — it proves the server-side
  calculation from the code walkthrough.
- If something doesn't appear, switch tabs and back, or restart the app (fresh data).
- Keep the terminal with **"Started HotelbookingApplication"** visible briefly so it's clear
  the live backend is responding.
- Speak in plain language — "I'm searching", "I'm booking", "I'm cancelling" — and tie each
  action back to the functionality number if you can.

---

## Quick reference — functionality → action

```
#5 Search available rooms → Stay tab: dates + Find a room
#1 Create a booking       → Book this room → form → Confirm booking
#2 Retrieve bookings      → Reservations tab (list) / Rooms tab
#3 Update a booking       → Reservations: Edit → Save changes
#4 Cancel a booking       → Reservations: Cancel → confirm
```
