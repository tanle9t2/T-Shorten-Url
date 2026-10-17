# Implementation Playbook - t-shorten-url

Project-specific patterns for the `java-pro` skill. Stack: Spring Boot 4.1, Java 21, MongoDB, Redis (`StringRedisTemplate`), Kafka, Bucket4j, MapStruct, Lombok, S3 + zxing (QR codes).

Package root: `com.tanle.t_shorten_url`. Layers: `controller` -> `service` / `service.impl` -> `repository`, with `cache`, `kafka`, `schedule`, `interceptor` alongside.

## 1. Conventions to follow

- Constructor injection via `@RequiredArgsConstructor`; `@Slf4j` for logging.
- Every service has an interface in `service` and an implementation in `service.impl`. Cache services follow the same split in `cache` / `cache.impl`.
- DTOs are split into `request` and `response`; entity <-> DTO conversion goes through MapStruct in `mapper`.
- Missing resources throw `ResourceNotFoundException`; `ControllerExceptionHandler` maps exceptions to `ExceptionResponse`.
- Comments: English only, explain why not what, no javadoc on ordinary methods.
- Redis key formats in use: `url:<code>`, `lock:url:<code>`, `analytics:view` (hash), `analytics:view:flushing`, `short-url:ip:<ip>` (rate limit).

## 2. Cache-aside redirect (`ShortUrlServiceImpl.findRedirectUrl`)

Order of checks:

1. `getShortUrl(code)` hit: an empty string means known-missing, so throw `ResourceNotFoundException`; otherwise publish a click event and return.
2. Miss: `acquireLock("url:" + code)`.
   - Winner: load from Mongo. If absent, `cacheNotFound` (5 min TTL, penetration guard) and throw. Otherwise `createShortUrl` (24h + random jitter, avalanche guard), publish, return. Always release the lock in `finally`.
   - Loser: sleep 500ms, re-read the cache, fail if still absent.

Rules when touching this flow:

- Keep the three defences: null-value caching (penetration), lock on miss (breakdown), TTL jitter (avalanche).
- Publish the `ClickEvent` on every successful redirect path, including the loser path.
- The commented-out `synchronized` block is intentionally dead: a JVM lock does not work across instances. Do not revive it.

Java 21 note: `Thread.sleep(500)` is cheap only on virtual threads. If the loser path becomes hot, enable `spring.threads.virtual.enabled=true` rather than adding a pool.

## 3. Distributed lock (`DistributeLockServiceImpl`)

`SET lock:<key> <uuid> NX EX 5` via `setIfAbsent`.

Known gap: `releaseLock` deletes without checking the owner value. If the work outlives the 5s TTL, the release can delete another instance's lock. Safe fix is a compare-and-delete Lua script keyed on the stored UUID. Keep the `acquireLock` / `releaseLock` signatures stable when fixing.

## 4. Analytics pipeline (Kafka)

`ShortUrlProducer` -> topic `${app.kafka.short-analyzed-topic}` -> `ShortUrlConsumer` (batch listener, group `analyzed-group`).

- `@RetryableTopic(attempts = "3", backOff = 2000, dltTopicSuffix = ".DLT")` gives retry topics plus a dead-letter topic.
- The consumer does two side effects per event: store a `ShortAnalyzed` document, then `HINCRBY analytics:view <code> 1`.
- `ClickEvent` is a record carrying `eventId` (UUID). Use `eventId` for idempotency: a retry replays the whole batch, so the Mongo insert and the Redis increment can be applied twice for events processed before the failure.
- Dedupe pattern to use: `setIfAbsent("event:<eventId>", "1", Duration.ofHours(1))` on Redis, skipping the event when it returns false. Place the check before both side effects.
- Check `ShortUrlConsumer` before changing duplicate handling; the working tree has uncommitted edits there.

## 5. Write-behind view counter

- Write path: consumer increments the Redis hash `analytics:view`.
- Flush path (`ShortUrlSchedule`, every 60s): read the hash, `RENAME` to `analytics:view:flushing`, write to Mongo, delete the flushing key.
- Read path (`getTotalView`): Mongo views + Redis counter.

Known gaps (verify before relying on the numbers):

- The flush calls `shortUrl.setViews(viewsCount)`, which overwrites the Mongo total with the delta. Combined with `getTotalView` adding the Redis counter, totals drop after each flush. The write should be an increment (`$inc`) of the delta.
- Increments that land between `entries()` and `rename()` in `getViewToFlush` are not in the snapshot but are renamed away. Do `RENAME` first, then read the renamed key.
- If the flush fails after `RENAME`, the flushing key is overwritten by the next rename. Merge or retry the leftover key before renaming again.

## 6. Rate limiting

`RateLimitInterceptor` (registered in `config/WebConfig`) calls `RateLimitService.tryConsume("short-url:ip:" + ip)`. Buckets are Bucket4j on Redis (`config/Bucket4jConfig`). On rejection it returns 429 with `Retry-After: 60`.

- `getRemoteAddr()` returns the proxy address behind a load balancer. If deployed behind one, set `server.forward-headers-strategy` instead of parsing `X-Forwarded-For` by hand.
- Keep the `Retry-After` value consistent with the configured refill period.

## 7. Short code creation

`SnowflakeIdGenerator.nextId()` (worker id from `worker.id`) -> `ShortCodeGenerator.encode(id)`. Each instance needs a distinct `worker.id`, otherwise codes can collide.

Known gap: `updateShortUrl` generates a new code and then calls `invalidCache(code)` with the new code. The stale entry under the old code stays cached. Capture the old code before reassigning and invalidate that one.

## 8. Testing

Only the context-load test exists (`TShortenUrlApplicationTests`). Add tests following the AAA pattern:

| Target | Style | Notes |
|---|---|---|
| `findRedirectUrl` | Unit, Mockito | Cover hit, empty-string hit, lock winner, lock loser, not found |
| `ShortCodeGenerator` | Unit | Round-trip and uniqueness for sequential ids |
| `SnowflakeIdGenerator` | Unit | Monotonic ids, no duplicates across threads |
| Consumer + dedupe | Integration, Testcontainers (Kafka, Redis, Mongo) | Replay the same `eventId`, assert one increment |
| Flush job | Integration | Assert totals after two flush cycles |

Run one test: `./gradlew test --tests "com.tanle.t_shorten_url.SomeTest.someMethod"`.

## 9. Config pitfalls

- `server.port` is 8081 but `app.domain` is `http://localhost:8080`, so returned short links point at the wrong port locally.
- App startup needs `S3_BUCKETNAME`, `AWS_REGION`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`.
- MongoDB is not in `docker-compose.yml`; it must run on `localhost:27017`.
- `.env` is staged in git. Do not commit real AWS keys.
