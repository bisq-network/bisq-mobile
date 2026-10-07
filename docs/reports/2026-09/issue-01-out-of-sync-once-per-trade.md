# Analytics: count out-of-sync trades once per trade, not once per session

The September 2026 monthly report shows 308 `trade.out_of_sync_detected` events against 253 trades started (289 on Bisq Easy, 19 on Bisq Connect Android, 0 on iOS). The number cannot be read as "trades that got stuck": the event is deduplicated in memory only (`outOfSync` set in `TradeAnalyticsTracker`), so the same stuck trade is counted again on every app restart, and stuck users restart a lot.

Today the report can only say "how often users face a stuck trade". What we need to prioritise the stuck-trade work is "how many trades got stuck" and "how many of those recovered".

### Proposed work

1. Persist "already reported as out of sync" per trade next to the stall clock (`TradeStallClockEntry` in `TradeStallClockRepository`), so the detection is emitted once per trade across restarts. The entry is already evicted when the trade is no longer open, so the store stays bounded.
2. Emit it under a new name (e.g. `trade.out_of_sync_first_detected`) instead of changing the meaning of `trade.out_of_sync_detected`. Old app versions keep sending the per-session event for months (0.11.0 still produced 16% of Bisq Easy events in September), and one name with two meanings would make every month-over-month comparison wrong. New versions send only the new event.
3. Emit `trade.out_of_sync_recovered` when a trade that was reported out of sync leaves `INIT`. Detected minus recovered is the number of trades that stayed stuck, which is the figure we actually want.
4. Update `scripts/monthly-report/report.py` to show: trades detected out of sync, share of trades started, and recovered vs still stuck. Keep the per-session event as a separate "older versions" row until it fades out.

### Acceptance

- A trade stuck in `INIT` across several app restarts produces exactly one detection event.
- A trade that later leaves `INIT` produces exactly one recovered event.
- Covered in `TradeAnalyticsTrackerTest` (restart survival, eviction, recovery).
- Privacy contract unchanged: sealed slugs only, no trade id, amount or peer.

### Non-goals

- Changing the detection threshold (10 minutes in `TradeOutOfSyncDetector`) or the recovery pane.
- Fixing the underlying desync; this only makes it measurable.

Refs: wiki 2026 Monthly Reports (September), `TradeAnalyticsTracker`, `TradeOutOfSyncDetector`, `TradeStallClockRepository`.
