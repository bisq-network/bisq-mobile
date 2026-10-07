# Analytics: record when an offer is actually published

The create-offer path can be followed screen by screen up to its review screen, and then goes dark: there is no event for an offer being published. In September 2026 the create-offer flow was started 867 times and its review screen was viewed 251 times, but we cannot say how many offers were created, nor how many attempts failed.

This also leaves a blind spot on the supply side. Trades started fell from 459 to 253 in September while offerbook visits held steady; without an offer-published count we cannot tell whether fewer offers were available to take.

### Proposed work

1. Add an `AnalyticsEvent.Offer` family and emit `offer.created` when `CreateOfferCoordinator.createOffer()` succeeds, and `offer.create_failed` when it returns a failure (also capturing the exception, like the trade step failures do).
2. Bake the direction into the name (`offer.created_buy` / `offer.created_sell`) so maker supply can be read per side. Nothing else: no market, amount, price or payment method.
3. Optional, same family: `offer.deleted` when the user removes their own offer, so offers published can be compared with offers withdrawn.
4. Update `scripts/monthly-report/report.py`: close the create-offer path with "offers published" (and failures), and add offers published to the month-over-month table.

### Acceptance

- Publishing an offer emits exactly one `offer.created_*`; a failed publish emits `offer.create_failed` and no created event.
- New slugs listed in the family's `all` so the contract test pins them.
- Privacy contract unchanged: sealed slugs only, no offer id, market, amount or price.

### Non-goals

- Tracking offers created outside the mobile apps, or how long an offer stays in the offerbook.

Refs: wiki 2026 Monthly Reports (September), `CreateOfferCoordinator`, `CreateOfferReviewPresenter`, `AnalyticsEvent`.
