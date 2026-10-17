# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Gradle project (Spring Boot 4.1, Java 21 toolchain). Use `gradlew.bat` on Windows.

- Infra (Redis, Kafka, Kafka UI, RedisInsight): `docker compose up -d`. MongoDB is NOT in the compose file; `application.yaml` expects it at `localhost:27017`.
- Run: `./gradlew bootRun`
- Build: `./gradlew build`
- All tests: `./gradlew test`
- Single test: `./gradlew test --tests "com.tanle.t_shorten_url.SomeTest.someMethod"`

`application.yaml` requires env vars `S3_BUCKETNAME`, `AWS_REGION`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY` (used by `config/S3Config` for QR code upload); the app will not start without them.

## Architecture

Single Spring Boot service, layered: `controller` -> `service`/`service.impl` -> `repository` (Spring Data MongoDB), with `cache` (Redis) and `kafka` alongside. MapStruct mappers live in `mapper`.

**Create flow:** `ShortUrlServiceImpl.save` builds a code from `SnowflakeIdGenerator` (worker id from `worker.id`) encoded by `ShortCodeGenerator`; optionally generates a QR code (zxing) and uploads it via `UploadService` (S3).

**Redirect flow** (`ShortUrlServiceImpl.findRedirectUrl`, the core of the app): cache-aside on Redis.
1. Cache hit -> return; an empty-string value means "known missing" (penetration guard, short TTL).
2. Miss -> acquire Redis lock `url:<code>` (`DistributeLockService`); the winner loads from Mongo and fills the cache (TTL 24h + random jitter). Losers sleep 500ms and re-read the cache.
3. Every successful redirect publishes a `ClickEvent` to Kafka.

**Analytics pipeline:** `ShortUrlProducer` -> topic `app.kafka.short-analyzed-topic` -> `ShortUrlConsumer` (batch listener, `@RetryableTopic` 3 attempts + `.DLT`). The consumer stores a `ShortAnalyzed` document and increments a Redis view counter (write-behind). `ShortUrlSchedule` flushes the counters from Redis to Mongo every 60s. `getTotalView` returns Mongo views plus the not-yet-flushed Redis counter. Consumer-side duplicate-message protection and `RateLimitInterceptor` (Bucket4j on Redis, registered in `config/WebConfig`) are recent additions; check those files before touching either.

Note: `server.port` is 8081 but `app.domain` (used to build returned short links) is `http://localhost:8080`.

## Conventions (from .claude/rules)

- Code comments: English only, explain *why* not *what*; no method javadoc for ordinary methods; TODOs only if tied to a real task.
- Project rules reference an `.agents/` directory (memory, checklist scripts) that does not currently exist in the repo; do not block on it.

## Workflow (spec-driven)

- Features follow spec-driven development: `specs/<NNN-name>/spec.md` -> `plan.md` -> `tasks.md` -> code. Keep the specs updated as documentation; record shortcuts in `specs/TECH-DEBT.md`.
- After implementing a feature or task set, always run the `code-reviewer` subagent on the changes before reporting it as done, then address or explicitly report its findings.
