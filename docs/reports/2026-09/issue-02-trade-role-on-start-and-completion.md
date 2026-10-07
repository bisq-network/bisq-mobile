# Analytics: record the user's role on trade start and completion

The trade funnel's headline number is completed ÷ started, but "started" (`trade.taken`) is only emitted for the user who takes an offer (`ClientTradesServiceFacade` / `NodeTradesServiceFacade` `takeOffer`). A trade where the user is the maker has no start event at all, yet it still emits `trade.completed`, `trade.errored`, cancels and rejects.

September 2026 shows the effect clearly on Bisq Connect Android: 3 started, 15 completed, 21 errored. Overall completion (74 of 253, 29%) is therefore slightly flattered, "still in progress" has to be estimated per app, and we cannot say how takers and makers convert separately.

### Proposed work

1. Emit a start event for maker-side trades, e.g. `trade.started_as_maker`, the first time a trade with `bisqEasyTradeModel.isMaker` appears among the open trades. It must fire once per trade, not on every restart: open trades replay at startup, so the "already counted" marker has to be persisted (the stall-clock store already keeps one entry per open trade and is evicted with it).
2. Tag completion with the role: `trade.completed_taker` / `trade.completed_maker`. `report.py` counts completion by the `trade.completed` prefix, so the existing month-over-month series keeps working and old versions' plain `trade.completed` still aggregates.
3. Decide whether cancels, rejects and errors need the role as well. Suggestion: errors yes (`trade.errored_taker` / `trade.errored_maker`), cancels and rejects no — they already carry reason and stall bucket in the name, and a third dimension multiplies the wire names for little gain.
4. Update `scripts/monthly-report/report.py`: taker completion (completed_taker ÷ taken) and maker completion (completed_maker ÷ started_as_maker) as separate lines, and drop the "more completions than starts" footnote once old versions fade.

### Acceptance

- A maker-side trade produces one start event across app restarts.
- Completion events carry the role; totals by prefix match the previous counting.
- Covered in `TradeAnalyticsTrackerTest`; new slugs registered in `AnalyticsEvent.Trade.all` so the contract test pins them.
- Privacy contract unchanged: the role is a bounded slug, no trade id, amount or peer.

### Non-goals

- Buyer/seller split — the phase events (`trade.phase_buyer_*` / `trade.phase_seller_*`) already carry it.

Refs: wiki 2026 Monthly Reports (September), `TradeAnalyticsTracker`, `AnalyticsEvent.Trade`.
