# Analytics: record how first-time buyers leave the payout-address step

Since Bisq Easy 0.14.0 and Bisq Connect 0.10.0 the take-offer wizard offers first-time buyers on mainchain offers an optional payout-address step. The only signal it produces is the screen view (`screen.take_offer_btc_address_opened`, 171 in September 2026), and screen views are counted again on every re-entry (Back, the review notice's Edit, returning from the wallet guide).

So we cannot answer the questions the step was built for: how many first-time buyers prepare an address up front, how many skip, how many open the wallet guide, and how many leave the wizard here. The first weeks of data show review-screen views falling on 0.14.x while the review-to-start ratio held at about 50%, and payout address confirmed after start rising to about 80% (73% on 0.13.0). That is consistent with first-time buyers now leaving one step earlier instead of after committing, but with 49 trades on 0.14.x it cannot be told apart from noise.

### Proposed work

1. In `TakeOfferBtcAddressPresenter.onNext()` emit one of two outcome events: `take_offer.btc_address_provided` (a valid address was staged) or `take_offer.btc_address_skipped` (the field was blank). Outcomes are per tap on Next, so unlike the screen view they are not inflated by re-entries.
2. Emit `take_offer.btc_address_wallet_guide_opened` when the wallet guide is opened from this step, to see whether "I need a wallet first" is what stops people.
3. Emit `take_offer.btc_address_abandoned` when the wizard is closed (X) from this step. Back is not an abandon: the user is still in the wizard.
4. Update `scripts/monthly-report/report.py` with a small block under "Path to a trade": provided / skipped / wallet guide / abandoned, next to payout address confirmed after start per app version.

### Acceptance

- Each Next tap on the step emits exactly one outcome event; an invalid non-blank address emits nothing (Next is disabled).
- New slugs registered so the analytics contract test pins them.
- Privacy contract unchanged: outcome slugs only, never the address or anything derived from it.

### Non-goals

- Changing the step's UX. This is measurement for the planned readout one month after release; any UX change follows from that data.

Refs: wiki 2026 Monthly Reports (September), `TakeOfferBtcAddressPresenter`, `TakeOfferCoordinator.showBtcAddressScreen()`.
