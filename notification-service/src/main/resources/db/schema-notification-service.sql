-- Notification Service database schema

CREATE TABLE notifications (
                               notification_id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               booking_id        INT, -- references Booking Service's bookings.booking_id, no real FK (different service DB) — matches its INT id type
                               user_id           UUID NOT NULL, -- references User Service's users.user_id, no real FK (different service DB)
                               type              VARCHAR(30) NOT NULL
                                   CHECK (type IN ('BOOKING_CONFIRMED', 'BOOKING_PREEMPTED', 'BOOKING_CANCELLED')),
                               status            VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                                   CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
                               resend_id         VARCHAR(255), -- id returned by Resend, for cross-referencing their dashboard
                               created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                               sent_at           TIMESTAMPTZ
);

CREATE INDEX idx_notifications_booking ON notifications(booking_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);