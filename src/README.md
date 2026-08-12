# URL Shortener Service

A backend URL shortening service built in Java with Javalin and PostgreSQL. Built as a learning project focused on understanding backend fundamentals from the ground up — raw JDBC, connection pooling, concurrency safety, and HTTP semantics — before moving to a framework like Spring Boot.

## Features

- **Short code generation** — random, collision-safe short codes generated with `SecureRandom`, with automatic retry on collision handled atomically at the database level (`ON CONFLICT DO NOTHING`), not via application-level checking.
- **Idempotent creation** — submitting the same long URL multiple times always returns the same short code, enforced by a database unique constraint rather than a check-then-insert pattern, so it holds correctly even under concurrent requests.
- **Link expiration** — URLs can optionally be created with a TTL; expired links return `410 Gone` instead of redirecting.
- **Click tracking** — click counts are incremented atomically at the database level (`click_count = click_count + 1` in a single statement) to avoid lost updates under concurrent access.
- **Rate limiting** — per-IP request limiting on the creation endpoint, backed by an in-memory sliding window.
- **Caching** — a Caffeine-backed read-through cache in front of short code lookups, using access-based expiration so frequently visited links stay cached.
- **Connection pooling** — HikariCP-managed connection pool instead of raw per-request JDBC connections.
- **Environment-based configuration** — database credentials and base URL are read from environment variables, with local-dev fallbacks, so no secrets are hardcoded or committed.

## Tech Stack

- **Language:** Java 25
- **Web framework:** Javalin
- **Database:** PostgreSQL
- **DB access:** raw JDBC (no ORM, by design — see "Why raw JDBC" below)
- **Connection pooling:** HikariCP
- **Caching:** Caffeine
- **Build tool:** Maven

## Architecture

```
org/example/
├── Main.java              # application entry point, wires everything together
├── config/
│   └── EnvUtil.java        # shared environment-variable helper
├── db/
│   ├── Database.java        # owns the HikariCP connection pool (singleton)
│   └── UrlRepository.java   # all SQL access — inserts, lookups, click counting
├── model/
│   ├── UrlRecord.java        # DB row representation (long URL + expiration)
│   └── ShortenReq.java       # incoming request body shape
├── service/
│   ├── UrlService.java       # short code generation + collision retry logic
│   └── RateLimiter.java      # per-IP rate limiting
├── web/
│   ├── UrlController.java    # HTTP handlers — request/response only, no business logic
│   └── HomeHandler.java
```

The layering follows a repository → service → controller pattern, hand-built with plain Javalin rather than relying on a framework's dependency injection or auto-configuration — deliberately, to understand what each layer is doing before using something like Spring Boot that does it automatically.

## API

### `POST /api/shorten`
Creates a short URL.

**Request body:**
```json
{
  "url": "https://example.com",
  "expiresInSecond": 3600
}
```
`expiresInSecond` is optional — omit it for a link that never expires.

**Response:**
```json
{
  "shortUrl": "http://localhost:7070/aB3xY9k"
}
```

Returns `400` if `url` is missing or not a valid `http(s)` URL.
Returns `429` if the per-IP rate limit is exceeded.

### `GET /{code}`
Redirects to the original URL.

- `302 Found` with a `Location` header, on success.
- `404 Not Found` if the short code doesn't exist.
- `410 Gone` if the short code exists but has expired.

## Concurrency guarantees

Two race conditions are handled explicitly, not just assumed away:

1. **Short code collisions** — two concurrent requests generating the same random short code are resolved atomically via a database unique constraint + `ON CONFLICT DO NOTHING`, rather than a `SELECT`-then-`INSERT` check in application code (which would have a race window).
2. **Duplicate long URLs** — the idempotency guarantee above holds under concurrency for the same reason.

Both were verified, not just reasoned about — a load test firing 50 concurrent requests for the same long URL was used to confirm the database never produced duplicate rows.

## Why raw JDBC (no ORM)

This project intentionally uses plain JDBC instead of an ORM like Hibernate/JPA, and Javalin instead of Spring Boot, to understand what those tools automate before relying on them. Connection pooling, SQL, and route registration are all written explicitly rather than hidden behind annotations.

## Setup

**Requirements:** JDK 25, Maven, Docker

1. Start Postgres:
   ```bash
   docker run --name url-shortener-db \
     -e POSTGRES_USER=devuser \
     -e POSTGRES_PASSWORD=devpass \
     -e POSTGRES_DB=urlshortener \
     -p 5432:5432 \
     -d postgres:16
   ```

2. Create the schema:
   ```sql
   CREATE TABLE urls (
       id BIGSERIAL PRIMARY KEY,
       short_code VARCHAR(10) UNIQUE NOT NULL,
       long_url TEXT UNIQUE NOT NULL,
       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       expires_at TIMESTAMPTZ,
       click_count BIGINT NOT NULL DEFAULT 0
   );
   CREATE INDEX idx_short_code ON urls (short_code);
   ```

3. Set environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `BASE_URL`) — see `EnvUtil` for defaults used if unset.

4. Run `Main.java`. The server starts on port `7070`.

## Possible next steps

- Deployment (Docker Compose + a hosted Postgres instance)
- Negative caching for repeated lookups of nonexistent codes
- Write-time cache population (currently cache-on-first-read only)