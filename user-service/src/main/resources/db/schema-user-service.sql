-- User Service database schema

CREATE TABLE users (
                       user_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name        VARCHAR(255) NOT NULL,
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       -- Higher value = higher priority. Mirrored into Booking Service's
                       -- user_tier_cache (also INT) via the UserTierChanged event.
                       tier        INT NOT NULL,
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- No explicit indexes needed. PRIMARY KEY on user_id and UNIQUE on email
-- each create a B-tree index automatically, so declaring them again would
-- only add write cost on every INSERT with nothing reading the duplicates.