-- Booking Service database schema
-- Connect with: psql -U scoot -d "booking-service"  (no USE statement in Postgres)
-- Quotes are required: the database name contains a hyphen.

CREATE TABLE rooms (
    room_id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    capacity    INT NOT NULL CHECK (capacity > 0),
    is_active   BOOLEAN NOT NULL DEFAULT true,
    is_occupied BOOLEAN NOT NULL DEFAULT false
    -- consider dropping this: occupancy depends on the current time vs. an
    -- active booking, so a static flag can drift out of sync unless something
    -- keeps updating it. Querying bookings directly is safer.
);

-- Local read-only cache of user tiers, kept in sync via the UserTierChanged
-- RabbitMQ event published by User Service. Avoids a cross-service network
-- call inside the lock-protected critical section during preemption evaluation.
CREATE TABLE user_tier_cache (
    user_id     UUID PRIMARY KEY, -- mirrors User Service's users.user_id, no real FK (different service DB)
    -- INT, matching User Service's users.tier. Higher value = higher priority,
    -- so the "highest tier wins" tie-break is a plain integer comparison.
    tier        INT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bookings (
    booking_id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      UUID NOT NULL, -- references User Service's users.user_id, no real FK (different service DB)
    room_id      INT NOT NULL REFERENCES rooms(room_id),
    start_time   TIMESTAMPTZ NOT NULL,
    end_time     TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'ACTIVE', 'PREEMPTED', 'CANCELLED', 'COMPLETED')),
    gap          INT NOT NULL, -- frozen at submission: start_time minus submitted_at, in minutes
    CHECK (end_time > start_time)
);

-- Speeds up the overlap-check query that runs inside the advisory lock
CREATE INDEX idx_bookings_room_time ON bookings(room_id, start_time, end_time);
CREATE INDEX idx_bookings_status ON bookings(status);

CREATE TABLE preemption_log (
    log_id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id                INT NOT NULL REFERENCES rooms(room_id),
    slot_datetime          TIMESTAMPTZ NOT NULL, -- anchored to the occupant's start time, so a whole challenger chain groups under the same cap count
    preempted_booking_id   INT NOT NULL REFERENCES bookings(booking_id),
    preempting_booking_id  INT NOT NULL REFERENCES bookings(booking_id),
    decision_reason        VARCHAR(20) NOT NULL
                            CHECK (decision_reason IN ('URGENCY_WIN', 'TIER_WIN')),
    preempted_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The hot query path: "how many preemptions already happened for this room+slot?"
-- runs on every conflict, while the advisory lock is held.
CREATE INDEX idx_preemption_room_slot ON preemption_log(room_id, slot_datetime);
