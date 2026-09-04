# Scoot — Progress Report

**Project:** Meeting Room Booking System (Backend)
**Thesis:** Rancang Bangun Backend API Penjadwalan Sumber Daya Terdistribusi (Sistem Booking Ruangan Meeting) dengan Arsitektur Event-Driven Microservices, menggunakan Preemptive Priority Scheduling Algorithm
**Author:** Intan · **Supervisor:** Pak Pandu · **Semester:** 7–8
**Repo:** https://github.com/IntanWp/Scoot
**Last updated:** 21 August 2026

---

## 1. Current status at a glance

| Build step | Status |
|---|---|
| 1. Scaffold services | Done |
| 2. Local infrastructure | Done |
| 3. **User Service** | **In progress — ~60%** |
| 4. Booking Service CRUD | Not started |
| 5. Preemption algorithm | Not started |
| 6. Notification Service | Not started |
| 7. Test suite (Testcontainers) | Not started |
| 8. Deploy + evaluate | Not started |

> **user-service does not currently compile.** Three known errors, all small. See §7.

---

## 2. Stack

| Component | Choice | Why |
|---|---|---|
| Framework | Spring Boot **4.0.7** | Must stay on 4.0.x — the MyBatis Spring Boot starter is not yet compatible with 4.1.x |
| Language | Java 17 | |
| Persistence | **MyBatis 4.0.1** (not JPA) | Raw SQL is required for `pg_advisory_xact_lock` calls and overlap queries |
| Database | PostgreSQL 16 | One database per service |
| Messaging | RabbitMQ 3 (management image) | |
| Email | Resend | |
| Auth | `X-User-Id` header | No Spring Security — deliberate scope limit |

No SNAPSHOT dependency versions anywhere, for reproducibility at defense time.

---

## 3. Architecture

Three independent services, one database each.

| Service | Port | Owns | Role |
|---|---|---|---|
| User Service | 8081 | `users` | Publishes `UserTierChanged` on tier change |
| Booking Service | 8080 | `rooms`, `bookings`, `preemption_log`, `user_tier_cache` | The core service — preemption logic lives here |
| Notification Service | 8082 | `notifications` | Consumes booking events, calls Resend, logs delivery status only |

**Repo layout.** A root aggregator `pom.xml` (packaging `pom`, artifactId `scoot`) lists the three services as `<modules>`. It is a *pure aggregator* — no `<parent>`, no `<dependencies>` — so each service keeps `spring-boot-starter-parent` independently. `mvnw` at the root builds all three in one command.

**Two deliberate design points worth defending:**

- **`user_tier_cache` is a local read-only mirror** of user tiers, synced by RabbitMQ event. It exists so that tier lookups never require a cross-service network call inside the lock-protected critical section during preemption evaluation.
- **Booking times are free-form ranges, not fixed slots.** This makes the concurrency-correctness testing meaningful — real overlap detection rather than a trivial equality check.

---

## 4. The preemption algorithm

Evaluated in this order whenever a booking request conflicts with an existing one:

1. **Grace Window** — a booking starting within 15 minutes is fully protected and cannot be preempted by anyone. Prevents sudden room loss.
2. **Cap** — maximum 3 preemptions per room + slot, counted from `preemption_log`. Prevents unbounded preemption chains (thrashing and starvation).
3. **Urgency Check** — a request is urgent if the gap between submission and meeting start is ≤ 60 minutes.
   - Both urgent → smaller gap wins
   - One urgent → the urgent one wins
   - Neither urgent → higher tier wins
   - Full tie (same urgency, same tier) → the current occupant is retained

**Concurrency correctness** rests on `pg_advisory_xact_lock` — transaction-scoped, not session-scoped, chosen specifically because it is safe under PgBouncer transaction-mode pooling. A `btree_gist` overlap-exclusion constraint was considered and **deliberately dropped**: the advisory lock is the real mechanism, and unexplainable schema is a liability in a defense.

---

## 5. Database

Schema files are committed at `<service>/src/main/resources/db/schema-<service>.sql`. These are hand-written and commented — they are the source of truth, not pg_dump output.

Database names use **hyphens** (`booking-service`, `user-service`, `notification-service`) to match what already exists in DBeaver. Hyphens require double-quoting in raw SQL; JDBC URLs are unaffected.

### booking-service

- **`rooms`** — room_id (INT identity), name, capacity (CHECK > 0), is_active, is_occupied
- **`user_tier_cache`** — user_id (UUID), tier (**INT**), updated_at
- **`bookings`** — booking_id (INT identity), user_id (UUID), room_id (FK), start_time, end_time, submitted_at, status, gap
  - status CHECK: `PENDING` / `ACTIVE` / `PREEMPTED` / `CANCELLED` / `COMPLETED`
  - `gap` in minutes, **frozen at submission, never recalculated**
  - `CHECK (end_time > start_time)`
  - Indexes: `idx_bookings_room_time (room_id, start_time, end_time)`, `idx_bookings_status`
- **`preemption_log`** — log_id, room_id, slot_datetime, preempted_booking_id, preempting_booking_id, decision_reason, preempted_at
  - `room_id` is duplicated from `bookings` **on purpose** — avoids a join in the hot cap-check query held under the advisory lock
  - `slot_datetime` is anchored to the *occupant's* start time, so a whole challenger chain groups under one cap count
  - `decision_reason` CHECK: only `URGENCY_WIN` / `TIER_WIN`. Grace-window rejections, cap rejections and tie-break retentions are **not** logged, because no preemption occurred
  - Index: `idx_preemption_room_slot (room_id, slot_datetime)`

### user-service

- **`users`** — user_id (UUID, default `gen_random_uuid()`), name, email (UNIQUE), tier (INT), created_at
  - UUID is used for users specifically because `user_id` doubles as the `X-User-Id` auth value; sequential integers would make impersonation trivial
  - No explicit indexes — PRIMARY KEY and UNIQUE each create one automatically

### notification-service

- **`notifications`** — notification_id, booking_id (nullable), user_id, type, status, resend_id, created_at, sent_at
  - `type` CHECK: `BOOKING_CONFIRMED` / `BOOKING_PREEMPTED` / `BOOKING_CANCELLED`
  - `status` CHECK: `PENDING` / `SENT` / `FAILED`

### Pending: live DB is out of sync with the schema files

These edits were made to the `.sql` files after the tables were already created in DBeaver. Both affected tables are empty, so both statements are safe:

```sql
-- against booking-service
ALTER TABLE user_tier_cache ALTER COLUMN tier TYPE INT USING tier::integer;

-- against user-service
DROP INDEX idx_users_email;
DROP INDEX idx_users_id;
```

---

## 6. What has been built

### Step 2 — Local infrastructure (done)

- `docker-compose.yml` — Postgres 16 + RabbitMQ 3-management, credentials `scoot`/`scoot`, healthchecks on both, named volumes `postgres_data` / `rabbitmq_data`
- `docker/init-db/01-create-databases.sql` — creates all three databases automatically on a fresh volume, so the environment is reproducible from the repo alone
- All three `application.properties` configured: datasource, RabbitMQ, MyBatis
  - `mybatis.configuration.map-underscore-to-camel-case=true` — maps `user_id` → `userId`; without it every mapper result silently returns null
- Root `.gitignore` (`.env`, `.idea/`, `target/`, `*.iml`), plus a committed `.env.example` template
- `<finalName>${project.artifactId}</finalName>` in all three poms, so Maven output matches what the Dockerfiles expect
- `booking-service` renamed from the Initializr default `demo` throughout

### Step 3 — User Service (in progress)

| File | Status |
|---|---|
| `model/User.java` | Written — plain POJO, `@Data` |
| `mapper/UserMapper.java` | Written — annotation-based (`@Insert` / `@Select` / `@Update`), Java 17 text blocks |
| `model/dto/request/CreateUserRequest.java` | Written — `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max` |
| `model/dto/request/UpdateTierRequest.java` | Written |
| `exception/ErrorCodeEnum.java` | Written — catalog of errors → HttpStatus + message |
| `exception/UserException.java` | Written — carries one `ErrorCodeEnum` up the stack |
| `exception/ErrorResponse.java` | Written — `{code, message}` JSON body |
| `exception/UserExceptionHandler.java` | Written — `@RestControllerAdvice`, two handler methods |
| `service/UserService.java` | **Not written** |
| `controller/UserController.java` | **Not written** |

**Mapper style decision:** annotations for User Service (four trivial CRUD statements don't justify an XML file), but **XML for Booking Service** — the overlap query and the advisory-lock call are long, partly conditional, and will be quoted directly in the thesis. Readable formatted SQL in an XML file beats SQL buried in a Java annotation. Mixing both styles in one project is normal and defensible.

**Exception design.** Failures reach the handler through two different doors:

- `UserException` — thrown by your own service, deep in the call stack
- `MethodArgumentNotValidException` — thrown by Spring during argument binding, **before the controller method runs at all**

Both are caught by `UserExceptionHandler` and leave as the same `{code, message}` shape.

Why this matters beyond tidiness: **JMeter measures error rate.** Booking Service will reject requests *by design* (grace window, cap, tie-break). If those come back as 500s, the error-rate metric measures bugs instead of the algorithm. Every deliberate rejection must be a 409, not a 500.

---

## 7. Known issues — user-service will not compile

Three errors, all mechanical:

1. **`model/User.java`** — file sits at `.../user_service/model/` but declares `package com.model;`. Java requires the package to match the directory.
2. **`mapper/UserMapper.java`** — same problem: declares `package com.mapper;` and imports `com.model.User`. Both need to become `com.example.user_service.mapper` and `com.example.user_service.model.User`.
3. **`UserExceptionHandler`** — calls `errorDetail.getCode()`, but the field in `ErrorCodeEnum` was renamed to `status`, so Lombok now generates `getStatus()`. `getCode()` no longer exists.

Lower priority:

4. `UpdateTierRequest` has `@NotBlank` on a `UUID` field. `@NotBlank` only validates `CharSequence` — this throws `UnexpectedTypeException` at runtime. Use `@NotNull`, or drop the field entirely since the user ID belongs in the path, not the body.
5. `ErrorCodeEnum` fields could be `final`. `ErrorResponse` has unused imports.
6. `ResponseEntity<?>` could be `ResponseEntity<ErrorResponse>`.

---

## 8. Git state

| Branch | Status |
|---|---|
| `main` | In sync with `origin/main` |
| `user_service` | Current working branch, in sync with its remote |
| `config_init`, `user_service_init` | Old feature branches, already merged via PR |

Workflow in use: feature branch → pull request → `main`.

**Outstanding:** the working tree shows every file as modified because of CRLF/LF line-ending churn — the content diff is empty (1530 insertions, 1530 deletions, and nothing at all when carriage returns are ignored). A root `.gitattributes` has been written to fix this permanently. Still needs:

```
git add --renormalize .
git commit -m "chore: normalize line endings"
```

---

## 9. Immediate next steps

1. Fix the three compile errors in §7
2. `./mvnw clean compile` at the repo root — first successful build
3. Run the two pending SQL statements from §5
4. Write `UserService` — generate UUID, lowercase the email before insert (the `UNIQUE` constraint is case-sensitive, so `Intan@` and `intan@` would otherwise both be accepted), catch `DuplicateKeyException` → `EMAIL_ALREADY_EXISTS`, treat 0 rows updated as `USER_NOT_FOUND`
5. Write `UserController` — `POST /users`, `GET /users/{id}`, `GET /users`, `PATCH /users/{id}/tier`
6. Test all four endpoints in Postman, **including failure cases**: duplicate email → 409, unknown UUID → 404, `tier: 7` → 400
7. Only then add RabbitMQ: durable topic exchange `user.events`, routing key `user.tier.changed`, verify at `localhost:15672`

---

## 10. Open decisions

- [ ] Keep or drop `rooms.is_occupied` — it is time-dependent derived state that can drift out of sync, and the overlap query already answers the same question
- [ ] `user_tier_cache` cold start — a user who never changes tier never enters the cache. Recommended: also publish a `UserCreated` event, plus a fallback lookup that runs **before** acquiring the advisory lock, which keeps the "no network call in the critical section" claim intact
- [ ] Reject stale tier events using `user_tier_cache.updated_at` — RabbitMQ does not guarantee ordering across redeliveries, and out-of-order tier updates would skew the fairness results
- [ ] Dead-letter queue on the tier-cache queue — a dropped tier event silently corrupts every future preemption decision
- [ ] CHECK constraint bounding tier to 1–3 in both `users` and `user_tier_cache`
- [ ] Smoke-test `pg_advisory_xact_lock` in two DBeaver sessions before building step 5 on top of it

---

## 11. Deployment plan (not started)

- **Postgres → Neon** (leaning). ~2s cold start versus Supabase's ~30s, which matters for JMeter latency numbers
- **RabbitMQ → CloudAMQP** free "Little Lemur" tier
- **Secrets** via `.env` + `spring-dotenv` (`me.paulschwarz:spring-dotenv:5.1.0`). `.env.example` is committed; `.env` is gitignored

## 12. Evaluation plan

1. **Performance** — JMeter: latency, throughput, error rate, scalability
2. **Concurrency correctness** — the advisory lock prevents double-booking under concurrent load (Testcontainers)
3. **Algorithm behaviour and fairness** — Python scripts verifying the cap and grace window are never violated

The `code` field in `ErrorResponse` (e.g. `PREEMPTION_CAP_EXCEEDED`) is what makes metric 3 practical — the analysis script reads the rejection reason directly instead of pattern-matching on English error text.
