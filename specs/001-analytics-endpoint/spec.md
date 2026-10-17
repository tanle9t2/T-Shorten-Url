# Spec: Analytics endpoint (`GET /api/v1/analytics`)

**Status**: Implemented (T1-T5 done). userId defaults to `anonymous`; see specs/TECH-DEBT.md.

## Objective
Replace the mock in `T-Shorten-Url-FE/src/services/analyticsService.ts` with one real endpoint that returns the data behind `/app/analytics?period=7`: summary tiles (current vs previous period), activity series, top links.

The FE currently derives everything from `createdAt` + lifetime `views` of each link (cohort model) because no click history existed. The backend now stores one `ShortAnalyzed` document per click (`createdAt`, `shortCode`, `userId`), so the endpoint can return **real clicks per day**.

## Assumptions (correct me now)
1. Response is computed server-side; FE stops aggregating (`deriveAnalytics` is dropped or reduced to mapping).
2. Time zone is passed by the client (`tz`, IANA name) because day bucketing depends on it; default `UTC`.
3. User scoping uses `userId` request param like `/short-urls` (no auth exists yet).
4. Clicks come from `short_analyzed` (Mongo). Clicks not yet consumed from Kafka are not counted (eventually consistent, acceptable per architecture.md).
5. Referrer/country/device breakdowns stay out of scope.

## Contract
`GET /api/v1/analytics?userId=anonymous&period=7&tz=Asia/Ho_Chi_Minh`
or `...&from=2026-10-01&to=2026-10-07` (inclusive day keys; overrides `period`).

Validation (same as FE `dateRange.ts`): `period` in {7,30,90}, default 30; custom range `from <= to`, `from <= today`, length <= 365 days; `> 90` days buckets by week (Monday start). Invalid -> 400 via `ControllerExceptionHandler`.

```json
{
  "summary": {
    "current":  { "linksCreated": 12, "totalClicks": 340, "linksWithClicks": 9, "averageClicks": 28.3 },
    "previous": { "linksCreated": 8,  "totalClicks": 210, "linksWithClicks": 6, "averageClicks": 26.2 }
  },
  "activitySeries": [ { "key": "2026-10-04", "linksCreated": 2, "clicks": 41 } ],
  "topLinks": [ { "id": "..", "shortCode": "abc", "originalUrl": "..", "clicks": 90, "share": 0.26 } ]
}
```
`activitySeries` is zero-filled for every bucket in range. `topLinks`: top 5 by clicks in range, ties by newest `createdAt`; `share` = clicks / totalClicks (0 when total is 0). `averageClicks` = totalClicks / linksWithClicks (average clicks per link that got clicks, 0 when none), because linksCreated and clicks are different cohorts.

## Implementation sketch
- `AnalyticsController` -> `AnalyticsService`/`impl` -> Mongo aggregations on `short_analyzed` (group by `$dateToString` with `timezone`, `$match` on `shortCode $in owner's codes` + `createdAt`) and `short_url` (`linksCreated` per bucket).
- Needs compound index on `short_analyzed(shortCode, createdAt)`.
- Controller carries `@CrossOrigin("http://localhost:5173")` like `ShortUrlController`.
- Not on the redirect path; no latency impact.

## Commands
- Run: `./gradlew bootRun` (needs docker compose infra + MongoDB + S3 env vars)
- Test: `./gradlew test --tests "com.tanle.t_shorten_url.service.AnalyticsServiceTest"`

## Testing strategy
Unit tests for range/bucket/validation logic (zero-fill, week bucketing, previous-range, zero-division); one integration-style test for the Mongo aggregation if embedded Mongo is available, else service test with mocked repository.

## Boundaries
- Always: English comments explaining why only; validate params; follow `ApiResponse`/exception conventions.
- Ask first: new Mongo index, new dependency, changing FE types.
- Never: touch redirect/consumer flow, count unflushed Redis counters here.

## Success criteria
- `period=7` returns 7 zero-filled daily buckets for the given tz; previous range is the 7 days before.
- Invalid params return 400 with a message; empty account returns zeros and `topLinks: []`.
- FE page renders from the endpoint with no mock (FE change is a separate task).

## Resolved Decisions
1. Real clicks per day from `short_analyzed`; `linksCreated` stays as a second series.
2. `ClickEvent.userId` means the link owner, but until login exists the publisher sets `"anonymous"` (TD-001 in specs/TECH-DEBT.md). Analytics therefore resolves the owner's `short_url.shortCode`s first (`short_url.userId`), then matches `short_analyzed.shortCode`. When TD-001 is paid, the lookup can switch to `short_analyzed.userId`.
3. Backend only. Mongo index `short_analyzed(shortCode, createdAt)` (replaces the earlier userId index) as part of the work.
