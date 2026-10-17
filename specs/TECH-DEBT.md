# Tech Debt

| ID | Debt | Why it exists | Resolve when |
|---|---|---|---|
| TD-001 | `ClickEvent.userId` / `ShortAnalyzed.userId` is always `"anonymous"` (`AppConstant.ANONYMOUS_USER_ID`). Its intended meaning is the **link owner**. | No login: `findRedirectUrl` receives no user, and the cache-hit path only knows the original URL. | Login ships: pass the owner into `findRedirectUrl`/`publishMessage` (resolving the owner without adding a DB read to the cache-hit path, e.g. caching it with the URL). Old events stay `"anonymous"`. |
| TD-003 | Per-shortCode analytics derives `os`/`browser` at read time from the raw `userAgent` with a coarse substring classifier; `ShortAnalyzed.os/browser` stay empty. | No UA parser dependency, and spec 002 must not touch the consumer. | Click volume makes grouping by raw `userAgent` slow, or exact versions are needed: classify once in the consumer (or add a parser library) and group on stored fields. |
| TD-004 | `ShortAnalyzed.country` is never populated, so the `countries` breakdown is always `Unknown`. | Needs GeoIP lookup of `ipAddress` (new dependency/database). | Decide on a GeoIP provider; resolve at ingest in the consumer. |
| TD-002 | `ShortUrlServiceImpl.save` sets the link owner `userId` to `"anonymous"` (`AppConstant.ANONYMOUS_USER_ID`), and the analytics endpoint takes `userId` as an unauthenticated request param. Links created before this change still have `userId = "1"`. | No auth. | Login ships: take the owner from the security context and drop the request param. |
