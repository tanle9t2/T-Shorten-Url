# Plan: Analytics endpoint

Spec: [spec.md](spec.md)

## Decision: owner lookup while userId is anonymous
`ClickEvent.userId` is the link owner by intent, but `findRedirectUrl` gets no user yet, so the publisher sets `"anonymous"` (TD-001). Analytics resolves the owner's `shortCode`s from `short_url` (by `userId`, active only) and matches `short_analyzed.shortCode` with `$in`. No extra cost on the redirect path.

## Components (dependency order)
1. Done: `ClickEvent.userId` = anonymous from the publisher.
2. Index: `@CompoundIndex(def = "{'shortCode':1,'createdAt':1}")` on `ShortAnalyzed`.
3. Range logic (pure, unit-testable): parse `period|from|to|tz`, validate, previous range, bucket keys (day or Monday-week when > 90 days), zero-fill.
4. Repository aggregations (`MongoTemplate`):
   - clicks per bucket and per shortCode on `short_analyzed` (`$match` shortCode in owner's codes + createdAt in [prevFrom, to], `$group` by `$dateToString` with timezone).
   - links created per bucket and `linksWithClicks` need `short_url` filtered by userId + createdAt.
5. `AnalyticsService` assembles `summary`, `activitySeries`, `topLinks` (top 5, ties newest first).
6. `AnalyticsController` `GET /api/v1/analytics`, `@CrossOrigin("http://localhost:5173")`, wrapped like the other endpoints; invalid params -> 400 via `ControllerExceptionHandler`.

## Risks
- A link owner with very many links makes the `$in` list large; acceptable now, revisit with the volume strategy.
- Timezone-aware grouping needs Mongo 5+ `$dateToString` timezone; verify local Mongo version.
- Clicks on deleted (inactive) links are dropped because only active codes are resolved (intended).

## Verification checkpoints
After T1: send a click, confirm stored `userId` is `anonymous`. After task 3: unit tests green. After task 6: curl `period=7` returns 7 buckets.
