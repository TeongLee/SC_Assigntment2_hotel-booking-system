'use strict';

// ---- warm imagery per room style (Unsplash; gracefully degrades to a gradient) ----
const ROOM_IMAGES = {
    SINGLE: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80',
    DOUBLE: 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80',
    SUITE:  'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
    DELUXE: 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=800&q=80',
};
const FALLBACK_IMG = 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=800&q=80';

// ---- helpers ----

const $ = (sel) => document.querySelector(sel);

async function api(method, url, body) {
    const opts = { method, headers: {} };
    if (body !== undefined) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
    }
    const res = await fetch(url, opts);
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        // GlobalExceptionHandler returns { status, error, message, ... }
        const message = (data && data.message) || res.statusText;
        throw { status: res.status, message };
    }
    return data;
}

function toast(msg) {
    const el = $('#toast');
    el.textContent = msg;
    el.classList.remove('hidden');
    clearTimeout(toast._t);
    toast._t = setTimeout(() => el.classList.add('hidden'), 3000);
}

function money(n) {
    return 'RM ' + Number(n).toFixed(2);
}

function badge(value) {
    const cls = String(value).toLowerCase();
    const label = String(value).replace('_', ' ');
    return `<span class="badge ${cls}">${label}</span>`;
}

function roomImg(type) {
    const src = ROOM_IMAGES[type] || FALLBACK_IMG;
    return `<img src="${src}" alt="" loading="lazy" onerror="this.style.display='none'">`;
}

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ---- tab switching ----

document.querySelectorAll('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
        const name = tab.dataset.tab;
        document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t === tab));
        document.querySelectorAll('.panel').forEach((p) => p.classList.toggle('active', p.id === name));
        if (name === 'rooms') loadRooms();
        if (name === 'bookings') loadBookings();
    });
});

// ---- Rooms tab ----

async function loadRooms() {
    const box = $('#rooms-list');
    box.innerHTML = '<p class="empty">Loading rooms…</p>';
    try {
        const rooms = await api('GET', '/api/rooms');
        if (!rooms.length) { box.innerHTML = '<p class="empty">No rooms yet.</p>'; return; }
        box.innerHTML = rooms.map(roomCard).join('');
    } catch (e) {
        box.innerHTML = `<p class="empty">Couldn't load rooms — ${escapeHtml(e.message)}</p>`;
    }
}

function roomCard(r) {
    return `
        <article class="card">
            <div class="card-media">${roomImg(r.type)}${badge(r.status)}</div>
            <div class="card-body">
                <div class="card-title-row">
                    <h3>Room ${escapeHtml(r.roomNumber)}</h3>
                    <span class="card-type">${escapeHtml(r.type)}</span>
                </div>
                <div class="price-row">
                    <span class="price price-big">${money(r.pricePerNight)}</span>
                    <span class="price-unit">per night</span>
                </div>
            </div>
        </article>`;
}

// ---- Stay / search tab ----

$('#search-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const checkIn = $('#search-checkin').value;
    const checkOut = $('#search-checkout').value;
    const type = $('#search-type').value;
    const box = $('#search-results');
    const note = $('#search-note');

    // make sure the Stay panel is the one showing
    document.querySelector('.tab[data-tab="search"]').classList.add('active');
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === 'search'));
    document.querySelectorAll('.panel').forEach((p) => p.classList.toggle('active', p.id === 'search'));

    let url = `/api/rooms/available?checkIn=${checkIn}&checkOut=${checkOut}`;
    if (type) url += `&type=${type}`;

    box.innerHTML = '<p class="empty">Looking for open rooms…</p>';
    try {
        const rooms = await api('GET', url);
        if (!rooms.length) {
            note.textContent = `Nothing open for ${checkIn} → ${checkOut}. Try different dates.`;
            box.innerHTML = '<p class="empty">No rooms available for those dates.</p>';
            return;
        }
        note.textContent = `${rooms.length} room${rooms.length === 1 ? '' : 's'} open for ${checkIn} → ${checkOut}.`;
        box.innerHTML = rooms.map((r) => availableCard(r, checkIn, checkOut)).join('');
    } catch (e) {
        // 400 from bad dates
        note.textContent = e.message || 'Please check your dates.';
        box.innerHTML = `<p class="empty">${escapeHtml(e.message)}</p>`;
    }
});

function availableCard(r, checkIn, checkOut) {
    return `
        <article class="card">
            <div class="card-media">${roomImg(r.type)}${badge(r.status)}</div>
            <div class="card-body">
                <div class="card-title-row">
                    <h3>Room ${escapeHtml(r.roomNumber)}</h3>
                    <span class="card-type">${escapeHtml(r.type)}</span>
                </div>
                <div class="price-row">
                    <span class="price price-big">${money(r.pricePerNight)}</span>
                    <span class="price-unit">per night</span>
                </div>
                <div class="card-actions center">
                    <button class="btn-primary"
                        onclick="openBooking(${r.id}, '${escapeHtml(r.roomNumber)}', '${escapeHtml(r.type)}', '${checkIn}', '${checkOut}')">
                        Book this room
                    </button>
                </div>
            </div>
        </article>`;
}

// ---- Booking modal (create + edit) ----

const modal = $('#modal');

function openBooking(roomId, roomNumber, type, checkIn, checkOut) {
    $('#modal-title').textContent = 'Reserve your stay';
    $('#form-booking-id').value = '';
    $('#form-room-id').value = roomId;
    $('#form-room-label').textContent = `Room ${roomNumber} · ${type}`;
    $('#form-guest-name').value = '';
    $('#form-guest-email').value = '';
    $('#form-checkin').value = checkIn || '';
    $('#form-checkout').value = checkOut || '';
    $('#modal-submit').textContent = 'Confirm booking';
    showModal();
}
window.openBooking = openBooking;

function fillEditForm(b) {
    $('#modal-title').textContent = 'Edit reservation';
    $('#form-booking-id').value = b.id;
    $('#form-room-id').value = b.roomId;
    $('#form-room-label').textContent = `Room ${b.roomNumber} · ${b.roomType}`;
    $('#form-guest-name').value = b.guestName;
    $('#form-guest-email').value = b.guestEmail;
    $('#form-checkin').value = b.checkInDate;
    $('#form-checkout').value = b.checkOutDate;
    $('#modal-submit').textContent = 'Save changes';
    showModal();
}

function showModal() {
    $('#form-error').classList.add('hidden');
    modal.classList.remove('hidden');
}
function hideModal() { modal.classList.add('hidden'); }

$('#modal-cancel').addEventListener('click', hideModal);
modal.addEventListener('click', (e) => { if (e.target === modal) hideModal(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') hideModal(); });

$('#booking-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = $('#form-booking-id').value;
    const payload = {
        roomId: Number($('#form-room-id').value),
        guestName: $('#form-guest-name').value,
        guestEmail: $('#form-guest-email').value,
        checkInDate: $('#form-checkin').value,
        checkOutDate: $('#form-checkout').value,
    };
    const errBox = $('#form-error');
    errBox.classList.add('hidden');

    try {
        const result = id
            ? await api('PUT', `/api/bookings/${id}`, payload)
            : await api('POST', '/api/bookings', payload);
        hideModal();
        toast(`Reservation #${result.id} confirmed · ${money(result.totalPrice)} for ${result.nights} night${result.nights === 1 ? '' : 's'}`);
        document.querySelector('.tab[data-tab="bookings"]').click();
    } catch (e) {
        // 400 (validation/bad dates), 404 (missing room), 409 (conflict)
        errBox.textContent = e.message || 'Something went wrong.';
        errBox.classList.remove('hidden');
    }
});

// ---- Reservations tab ----

async function loadBookings() {
    const box = $('#bookings-list');
    const summaryBox = $('#booking-summary');
    summaryBox.innerHTML = summaryLoading();
    box.innerHTML = '<p class="empty">Loading reservations…</p>';
    try {
        const [summary, bookings] = await Promise.all([
            api('GET', '/api/bookings/summary'),
            api('GET', '/api/bookings'),
        ]);
        summaryBox.innerHTML = summaryCards(summary);
        if (!bookings.length) { box.innerHTML = '<p class="empty">No reservations yet. Find a room to get started.</p>'; return; }
        box.innerHTML = bookings.map(bookingCard).join('');
    } catch (e) {
        summaryBox.innerHTML = '';
        box.innerHTML = `<p class="empty">Couldn't load reservations — ${escapeHtml(e.message)}</p>`;
    }
}

function summaryLoading() {
    return `
        <article class="summary-card muted">
            <span class="summary-label">Summary</span>
            <strong class="summary-value">Loading</strong>
            <span class="summary-detail">Checking reservations</span>
        </article>`;
}

function summaryCards(s) {
    return `
        <article class="summary-card featured">
            <span class="summary-label">Total revenue</span>
            <strong class="summary-value">${money(s.totalRevenue)}</strong>
            <span class="summary-detail">Confirmed and active stays</span>
        </article>
        <article class="summary-card">
            <span class="summary-label">Reservations</span>
            <strong class="summary-value">${s.totalBookings}</strong>
            <span class="summary-detail">${s.confirmedBookings} confirmed</span>
        </article>
        <article class="summary-card">
            <span class="summary-label">Upcoming</span>
            <strong class="summary-value">${s.upcomingBookings}</strong>
            <span class="summary-detail">Arrivals from today onward</span>
        </article>
        <article class="summary-card">
            <span class="summary-label">Completed</span>
            <strong class="summary-value">${s.checkedOutBookings}</strong>
            <span class="summary-detail">${s.cancelledBookings} cancelled</span>
        </article>`;
}

function bookingCard(b) {
    return `
        <article class="card">
            <div class="card-media">${roomImg(b.roomType)}${badge(b.status)}</div>
            <div class="card-body">
                <div class="card-title-row">
                    <h3>Room ${escapeHtml(b.roomNumber)}</h3>
                    <span class="card-type">${escapeHtml(b.roomType)}</span>
                </div>
                <p class="guest-line">${escapeHtml(b.guestName)} · ${escapeHtml(b.guestEmail)}</p>
                <p class="date-line">${b.checkInDate} → ${b.checkOutDate} · ${b.nights} night${b.nights === 1 ? '' : 's'}</p>
                <div class="price-row">
                    <span class="price price-big">${money(b.totalPrice)}</span>
                    <span class="price-unit">total</span>
                </div>
                <div class="card-actions">
                    <button class="btn-text btn-edit" onclick='resolveAndEdit(${editPayload(b)})'>Edit</button>
                    <button class="btn-text btn-cancel" onclick="cancelBooking(${b.id})">Cancel</button>
                </div>
            </div>
        </article>`;
}

// BookingResponse has no roomId; stash the fields and resolve the id on edit.
function editPayload(b) {
    return JSON.stringify({
        id: b.id, roomNumber: b.roomNumber, roomType: b.roomType,
        guestName: b.guestName, guestEmail: b.guestEmail,
        checkInDate: b.checkInDate, checkOutDate: b.checkOutDate, roomId: null,
    });
}

// Resolve roomId (PUT needs it) by matching roomNumber, then open the edit form.
window.resolveAndEdit = async function (b) {
    if (b.roomId == null) {
        try {
            const rooms = await api('GET', '/api/rooms');
            const match = rooms.find((r) => r.roomNumber === b.roomNumber);
            b.roomId = match ? match.id : null;
        } catch (_) { /* fall through; PUT will surface a clear error */ }
    }
    fillEditForm(b);
};

window.cancelBooking = async function (id) {
    if (!confirm(`Cancel reservation #${id}?`)) return;
    try {
        await api('DELETE', `/api/bookings/${id}`);
        toast(`Reservation #${id} cancelled`);
        loadBookings();
    } catch (e) {
        toast(e.message || 'Could not cancel reservation.');
    }
};

// ---- shrink nav on scroll (subtle) ----
const nav = $('#nav');
window.addEventListener('scroll', () => {
    nav.style.boxShadow = window.scrollY > 20 ? '0 8px 24px rgba(64,43,28,0.10)' : 'none';
}, { passive: true });

// ---- initial load ----
loadRooms();
