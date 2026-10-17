# Spec: Per-shortCode statistics (`GET /api/v1/short-url/{code}/analytics`)

**Status**: Implemented and reviewed (T1-T7). os/browser derived from raw userAgent (TD-003); countries always Unknown (TD-004). Known: `userId` param is spoofable until auth (TD-001/002); total and breakdowns come from separate queries so may skew by in-flight clicks.

## Objective
Give the owner of one short link a detailed view of its traffic: clicks over time, change vs the previous period, and breakdowns by country, OS, browser and referer. Spec 001 (`/api/v1/analytics`) aggregates across all of a user's links; this endpoint drills into a single `shortCode`.

Data source is `short_analyzed` (one document per click, already carrying `country`, `os`, `browser`, `referer`, `createdAt`). Backend only; FE is a separate task.

## Decisions (from clarification)
1. Metrics: clicks per day, total vs previous period, breakdown by country/os/browser, top referers.
2. Path follows the existing `/short-url/{code}/views` style (singular `short-url`), under `/api/v1`.
3. Clicks come only from `short_analyzed`; unflushed Redis counters are not added (eventually consistent via Kafka).
4. Backend only.

## Contract
`GET /api/v1/short-url/{code}/analytics?userId=anonymous&period=7&tz=Asia/Ho_Chi_Minh`
or `...&from=2026-10-01&to=2026-10-07` (inclusive, overrides `period`).

Params and validation are identical to spec 001 and must reuse `AnalyticsRange` (period in {7,30,90} default 30; `from <= to <= today`; <= 365 days; > 90 days buckets by Monday-start week; `tz` IANA default `UTC`). Invalid -> 400 via `ControllerExceptionHandler`. Unknown code -> 404 (`ResourceNotFoundException`). Code owned by another `userId` -> same 404 (do not leak existence).

```json
{
  "shortCode": "abc",
  "originalUrl": "https://...",
  "summary": {
    "current":  { "totalClicks": 340 },
    "previous": { "totalClicks": 210 }
  },
  "clickSeries": [ { "key": "2026-10-04", "clicks": 41 } ],
  "countries": [ { "name": "VN", "clicks": 200, "share": 0.59 } ],
  "os":        [ { "name": "Windows", "clicks": 150, "share": 0.44 } ],
  "browsers":  [ { "name": "Chrome", "clicks": 260, "share": 0.76 } ],
  "referers":  [ { "name": "direct", "clicks": 120, "share": 0.35 } ]
}
```

Rules:
- `clickSeries` is zero-filled for every bucket in range.
- Each breakdown: top 5 by clicks in the current range, ties by name ascending. Remaining rows are folded into one `"Other"` row only if there are more than 5 distinct values. Null/blank values map to `"Unknown"`; for `referers` null/blank maps to `"direct"`. `share` = clicks / totalClicks (0 when total is 0).
- `referers` are reduced to the host (e.g. `https://www.google.com/search?q=x` -> `www.google.com`) so raw URLs with query strings are not exposed or fragmented. Unparseable values map to `"Unknown"`.
- Empty range returns zeros, a zero-filled series and empty breakdown arrays.

## Implementation sketch
- `AnalyticsController` gets a second mapping (or a small `ShortCodeAnalyticsController` if it keeps the file focused) -> `AnalyticsService.getShortCodeAnalytics(userId, code, period, from, to, tz)`.
- Resolve the link via `ShortUrlRepository` (exists + owner check) before any aggregation.
- `AnalyticsRepository`: Mongo aggregations on `short_analyzed` with `$match {shortCode, createdAt in [start,end)}`, then `$group` by `$dateToString` (with `timezone`) for the series, and one `$group` per dimension (can be a single `$facet` to do one pass). Referer host extraction is done in Java after grouping on raw referer, or via a pre-stored host if volume makes that costly (record in TECH-DEBT if deferred).
- Existing index `short_analyzed(shortCode, createdAt)` from spec 001 covers the `$match`; no new index.
- New response record `ShortCodeAnalyticsResponse` next to `AnalyticsResponse`; reuse `AnalyticsRange` for range, previous range and bucketing.
- Not on the redirect path; no latency impact.

## Commands
- Run: `./gradlew bootRun` (needs `docker compose up -d`, MongoDB, S3 env vars)
- Test: `./gradlew test --tests "com.tanle.t_shorten_url.service.ShortCodeAnalyticsServiceTest"`

## Code style
Match `AnalyticsServiceImpl`: Lombok `@RequiredArgsConstructor`, records for responses, `ApiResponse` wrapper, English comments explaining why only, no method javadoc.

## Testing strategy
- Unit (service, mocked repository): unknown code -> 404, foreign owner -> 404, zero-fill, previous-period total, share with zero total, top-5 + "Other" folding, null -> "Unknown"/"direct", referer host extraction.
- `AnalyticsRange` tests already exist; add none unless range logic changes.
- Integration against Mongo for the aggregation only if embedded/Testcontainers Mongo is already available; otherwise cover via the mocked service test and a manual check.

## Boundaries
- Always: validate params; reuse `AnalyticsRange`; owner check before aggregating.
- Ask first: new Mongo index, new dependency, changing `ShortAnalyzed` or `ClickEvent`.
- Never: touch the redirect/consumer flow, add Redis counters into these numbers, return raw referer URLs or IP addresses.

## Success criteria
- `period=7` returns 7 zero-filled daily buckets in the given tz; `previous.totalClicks` covers the 7 days before.
- Each breakdown sums (with "Other") to `current.totalClicks`.
- Wrong code or wrong owner -> 404; invalid params -> 400 with a message.

## Open points to confirm
1. `userId` is a request param defaulting to `anonymous` until auth exists (same as spec 001, TD-001). OK?
2. Is the click's `country` actually populated by the consumer today? If not, `countries` would be all `"Unknown"`; I will check `ShortUrlConsumer` during planning and log a tech-debt item rather than expand scope.
