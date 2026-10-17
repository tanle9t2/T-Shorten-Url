# Plan: Per-shortCode statistics

## Findings that change the spec's assumptions
- `ShortAnalyzed.country/os/browser` are **never populated**: `ClickEvent` only carries `ipAddress`, `userAgent`, `referer`, and `ShortAnalyzedMapper` copies just those. Only `userAgent`, `referer`, `ipAddress` exist in Mongo.
- `ShortUrlServiceImpl.publishMessage` writes the literal `"Unknow"` when the `Referer` header is absent.

## Decisions
1. **os / browser**: derive from the stored raw `userAgent` at read time. Mongo groups by `userAgent` (few distinct values), Java classifies each and sums. This covers historical clicks and keeps the consumer/redirect flow untouched (spec boundary). No new dependency: a small ordered substring matcher (`UserAgentClassifier`). Limit: coarse families only (Chrome, Edge, Firefox, Safari, Opera, Other / Windows, macOS, Linux, Android, iOS, Other). Recorded as TD-003.
2. **referer**: group by raw `referer`, then in Java map `null`/blank/`"Unknow"` -> `direct`, otherwise host via `URI`, unparseable -> `Unknown`; merge equal hosts.
3. **country**: no data source (needs GeoIP, i.e. a new dependency/database, "ask first"). The `countries` field stays in the contract but groups `ShortAnalyzed.country`, so it is `Unknown` until TD-004 is paid. No ingest change now.
4. **Single pass**: one `$facet` aggregation on `short_analyzed` with `$match {shortCode, createdAt in [previousStart, currentEnd)}` and branches `byDay`, `byUserAgent`, `byReferer`, `byCountry`. Branches other than `byDay` are restricted to the current range; the previous-range total comes from `byDay` keys (same trick as `clicksIn` in `AnalyticsServiceImpl`). If `$facet` proves awkward with `MongoTemplate`, fall back to separate aggregations; the repository method signature hides this.
5. **Owner / 404**: `ShortUrlRepository` lookup by code + `userId`; missing or foreign -> `ResourceNotFoundException`. Need to confirm during T1 which finder exists (`findByShortCode`-style) and whether soft-deleted links are excluded like `findAllByUserId`.

## Structure
- `util/UserAgentClassifier` (new, pure): `os(String)`, `browser(String)`.
- `util/BreakdownBuilder` or private helpers in the service: top-5 + `Other` folding, share, ordering (clicks desc, name asc).
- `repository/AnalyticsRepository`: add `ShortCodeBreakdowns clickBreakdowns(String code, Instant from, Instant toExclusive, Instant currentStart, ZoneId zone)` returning raw rows (`day`, `userAgent`, `referer`, `country` counts).
- `response/ShortCodeAnalyticsResponse` (new record, shape per spec).
- `service/AnalyticsService` + `AnalyticsServiceImpl`: `getShortCodeAnalytics(...)`.
- `controller/AnalyticsController`: new `GET /short-url/{code}/analytics` mapping (same class, same `@CrossOrigin`). Mapping does not collide with `ShortUrlController` (`/short-url/{id}` has no suffix).
- Reuse `AnalyticsRange` for validation, previous range, bucket keys.

## Risks
- `$facet` result is one document capped at 16MB; the per-day branch is bounded by <= 365 days and the group branches by distinct values, so fine for a single link.
- User-agent cardinality can be high for a viral link; acceptable now, noted as a limit in a `shortcut:` comment.
- Route `/short-url/{code}/analytics` must not be swallowed by `RedirectController` (check its mapping in T5).
