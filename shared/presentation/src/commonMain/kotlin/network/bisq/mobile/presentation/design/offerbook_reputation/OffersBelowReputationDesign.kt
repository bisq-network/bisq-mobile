/**
 * OffersBelowReputationDesign.kt — Design PoC (Issue #1873)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code. All preview
 * data flows through [simulatedOffersBelowReputationUiState] / [simulatedOffendingOfferRow],
 * which take only primitives.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Bisq2 Desktop checks the maker's OWN published sell offers against their CURRENT reputation
 * score on every offerbook activation
 * (`BisqEasyOfferbookController.onActivate()`, using
 * `BisqEasySellersReputationBasedTradeAmountService.hasSellerSufficientReputation`) and, if any
 * can no longer be taken, shows a popup listing what to do about it. Mobile validates
 * reputation only at create-offer time (client-side) and take-offer time
 * ([network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator.checkTakeOfferEligibility]);
 * nothing ever re-checks a maker's own already-published offers, so a seller whose score drops
 * (profile-age recalculation, bond/burn expiry, ranking changes) keeps live, unreachable offers
 * with no explanation. rodvar hits this on every release test.
 *
 * Production reference: `shared/presentation/.../offerbook/OfferbookPresenter.kt` +
 * `OfferbookScreen.kt` + `OfferCard.kt`. Desktop reference:
 * `bisq2/apps/desktop/desktop/.../bisq_easy/offerbook/BisqEasyOfferbookController.java` lines
 * ~250-278, backed by `bisq2/bisq-easy/.../BisqEasySellersReputationBasedTradeAmountService.java`.
 *
 * ======================================================================================
 * VERDICT — ARE THE OFFENDING OFFERS HIDDEN FROM OTHER USERS ON MOBILE? NO.
 * ======================================================================================
 * Desktop's popup message claims "As a result, these offers are hidden from other users" —
 * true on desktop, because `hasSellerSufficientReputation` is also used as an offerbook-list
 * FILTER for every message that is not the viewer's own (see the controller wiring; the
 * service itself is a `Service`, not just a popup helper). Mobile has NO equivalent filter:
 * grepping the whole `shared/` module for `SufficientReputation` / `ReputationBasedTradeAmount`
 * outside generated translation bundles returns nothing. `OfferbookPresenter.processOffer` only
 * computes [network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel.isInvalidDueToReputation]
 * for `DirectionEnum.BUY` offers (checking whether the VIEWER, as a prospective seller/taker,
 * clears the requirement) — it never evaluates a SELL offer's OWN maker score, and it never
 * removes anything from `sortedFilteredOffers`. So on mobile, a seller's below-reputation SELL
 * offers stay fully visible to every buyer, who reaches them, taps take-offer, and gets stopped
 * by their OWN [network.bisq.mobile.presentation.offer.take_offer.TakeOfferEligibility.NotEnoughReputation]
 * dialog when `checkTakeOfferEligibility` resolves the MAKER's score (SELL branch, line ~387 of
 * `TakeOfferCoordinator.kt`) — confirming the issue's premise exactly: buyers waste a tap
 * reaching an offer that was always going to reject them, and the seller never learns why.
 * **This PoC's copy must not claim the offers are hidden — doing so would tell the seller a
 * false safety property.** See "COPY" below for the resulting new message key.
 *
 * ======================================================================================
 * THE FORMULA (ported already; nothing new needed at the math layer)
 * ======================================================================================
 * Desktop: `withTolerance(sellersScore) < requiredReputationScoreForMinOrFixed`, where
 * `requiredReputationScoreForMinOrFixed` falls back to the max/fixed-amount score when the
 * offer has no separate min (fixed-amount offers). Mobile already has both halves ported to
 * `shared/domain/.../domain/utils/BisqEasyTradeAmountLimits.kt`:
 *   - [network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount]
 *     — already folds "fixed → same value for min and max" via `getFixedOrMinAmount()`, so one
 *     call reproduces desktop's `orElse(max)` fallback.
 *   - [network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.withTolerance] — already
 *     exists, unused by anything today; this PoC is its first real caller.
 * So the production check, per own SELL offer, is exactly:
 * ```
 * val required = BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
 *     marketPriceServiceFacade, offer.bisqEasyOffer, limits) ?: return@offer /* skip, can't compute */
 * val offending = BisqEasyTradeAmountLimits.withTolerance(myScore, limits) < required
 * ```
 * identical in shape to `TakeOfferCoordinator.checkTakeOfferEligibility`'s SELL-offer branch,
 * minus the tolerance (that check is strict; ours mirrors desktop's own popup gate, which does
 * apply tolerance — small buffer against boundary flapping right as the score changes).
 *
 * ======================================================================================
 * WHERE THIS LIVES: OfferbookPresenter, not a new dedicated presenter
 * ======================================================================================
 * `OfferbookPresenter` already owns every dependency this needs — `offersServiceFacade`
 * (`offersByAuthor`, `deleteOffer`), `reputationServiceFacade`, `marketPriceServiceFacade`,
 * `configServiceFacade.tradeAmountLimits` — and already owns the sibling concept for the OTHER
 * direction: `isInvalidDueToReputation` / `showReputationRequirementInfo` /
 * `showNotEnoughReputationDialog`. A dedicated presenter would duplicate all of that wiring for
 * no isolation benefit; this is additive state on the same presenter, named distinctly so it
 * cannot be confused with the existing (unrelated) not-enough-reputation-to-TAKE dialog:
 * [OffersBelowReputationUiState] / `showOffersBelowReputationDialog`.
 *
 * Fetch source: [network.bisq.mobile.data.service.offers.OffersServiceFacade.offersByAuthor] —
 * NOT `offerbookListItems`/`sortedFilteredOffers`, which are scoped to the currently selected
 * market. Desktop iterates every channel (every market); `offersByAuthor` is the one mobile call
 * that already does the same across markets (built for the peer-profile "Trade again" list —
 * see agent memory `project_peer_profile_trade_again_design`). Its `mayBeIncomplete` flag (true
 * while the client's all-markets offers cache is still syncing over Tor) must gate this check:
 * an incomplete snapshot must never fire the dialog on a false-negative pass (offer not synced
 * yet ≠ offer doesn't exist / is fine) — skip silently and let the next trigger (below) retry
 * once the cache settles, exactly like `isSyncingSelectedMarketOffers` already guards the list's
 * own empty state elsewhere in this file.
 *
 * ======================================================================================
 * TRIGGERS — "on offerbook entry and when own score changes" (issue's own words)
 * ======================================================================================
 * Two triggers, both already available on the presenter:
 *   1. `onViewAttached()` — entry. Desktop only checks `onActivate()`; mobile mirrors that as
 *      the baseline.
 *   2. A live collector on `reputationServiceFacade.scoreByUserProfileId`, filtered to my own
 *      profile id, `distinctUntilChanged()`, recompute on every change while the screen stays
 *      attached — this is the literal "when own score changes" half of the ask, and it is
 *      MORE proactive than desktop (which only invalidates its cache on score-change and waits
 *      for the next popup-eligible activation to actually show anything). Mobile users
 *      plausibly leave the offerbook open longer (background app, pull-to-refresh) than desktop
 *      users leave the controller un-reactivated, so the live trigger is worth the small extra
 *      reactivity; it reuses a flow the presenter would otherwise have to poll for regardless.
 *   3. A bounded re-query while the snapshot reports `mayBeIncomplete`. Neither trigger above
 *      fires when the all-markets cache finishes syncing, so without this an incomplete pass on
 *      entry would go unchecked until the score changes or the screen is re-entered. Mirror
 *      `PeerProfilePresenter.loadPeerOffers` exactly: re-run the check on the same interval and
 *      retry budget (`PEER_OFFERS_SYNC_RETRY_MS` / `PEER_OFFERS_SYNC_RETRIES`) while
 *      `mayBeIncomplete` is true; the query is a local cache read, no round trip. An incomplete
 *      pass still shows nothing, only the retry is added.
 *   Own-offer changes need no trigger of their own: create-offer validates the reputation limit
 *   client-side, so a new offer cannot start out offending, and the create/delete flows leave and
 *   re-enter the offerbook, which re-runs trigger 1. An offer removed elsewhere only shrinks the
 *   offending set, which the subset rule below already treats as nothing new.
 *
 * ======================================================================================
 * PER-SESSION DISMISSAL — WHAT COUNTS AS "THE SAME SITUATION" (DECIDED 2026-09-23)
 * ======================================================================================
 * The issue asks for dismissal "remembered per session so it doesn't nag on every visit."
 * A single boolean "dialog dismissed" flag is the wrong granularity: it would also suppress a
 * GENUINELY NEW problem (one more offer just became invalid after a further score drop, or a
 * newly created offer starts out invalid) for the rest of the session, which is not what
 * "don't nag about the SAME thing" should mean. This PoC keys dismissal to the exact SET of
 * offending offer ids the user chose "Keep" on (a sorted id list is enough — no need for a
 * hash): on the next trigger, if the freshly computed offending set is a SUBSET of (or equal
 * to) the last-dismissed set, stay silent; if it contains any id NOT in the last-dismissed set,
 * show the dialog again (with the full current list, not just the delta — the seller should
 * always see everything currently wrong, per desktop's own re-list-everything-every-time
 * behavior). "Session" = presenter/process lifetime, held as plain in-memory state (no
 * persistence layer) — consistent with `BisqEasyTradeAmountLimits.invalidBuyOffers`, the
 * sibling BUY-offer cache, which is also process-lifetime only. Removing an offer or building
 * enough reputation naturally shrinks the offending set on the next trigger, which is itself
 * a strict subset of anything previously dismissed, so it never re-nags for offers the user
 * already resolved.
 *
 * ======================================================================================
 * DIALOG — WHY LIST THE OFFERS (UNLIKE DESKTOP) AND HOW IT MAPS TO EXISTING PATTERNS
 * ======================================================================================
 * Desktop's popup is generic text; the issue explicitly asks mobile to list market + amount per
 * offer, which is more actionable on a screen with no persistent list of "my offers" visible
 * behind the dialog (desktop's offerbook table stays visible under a popup; mobile's dialog
 * covers the whole screen). [OffersBelowReputationDialog] reuses the existing
 * [network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog]
 * exactly as `OfferbookScreen.kt` already does for the sibling not-enough-reputation-to-TAKE
 * dialog on this same screen, using its `extraContent` slot (already built for
 * variable-length dialog bodies) to render the offending-offer rows between the message and the
 * sticky buttons — no new dialog primitive needed.
 *
 * Two-button contract, matching every other [ConfirmationDialog] on this screen (never three
 * buttons): confirm = "Remove offers" (primary, destructive-adjacent but NOT styled `.danger` —
 * deleting your own offer is a normal, reversible-by-recreating action, not the fraud-tier
 * severity of the banned-account design), dismiss = "Keep" (explicit, not "Cancel" — this is an
 * affirmative choice to leave the offers as they are, not an aborted operation; "Cancel" would
 * misdescribe it). Desktop's third action ("Learn how to build up reputation") is kept but
 * demoted to a tertiary underlined text link inside `extraContent`, below the offer list and
 * above the sticky buttons — [BisqButtonType.Underline], the same type [LinkButton] uses — so
 * the two-button contract this screen already establishes elsewhere is not broken by a third
 * full-width button.
 *
 * ======================================================================================
 * "LEARN HOW TO BUILD UP REPUTATION" — KEPT, NAVIGATES IN-APP (NOT THE WIKI)
 * ======================================================================================
 * Mobile already has a `ReputationScreen` (`NavRoute.Reputation`,
 * `CommonNavGraph.kt` line 166) and — critically — `OfferbookPresenter` already navigates
 * there for the EXACT SAME semantic situation from the take-offer side: when
 * `isReputationWarningForSellerAsTaker` is true (the viewer's own score is what's short), its
 * `ConfirmationDialog`'s confirm action is `onNavigateToReputation` → `navigateTo(NavRoute.Reputation)`
 * — in-app, NOT `BisqLinks.REPUTATION_WIKI_URL`. Only the OTHER branch (someone else's score is
 * short) opens the external wiki via [network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialog].
 * This PoC's situation — the viewer's OWN score is short — is that same "my own score" case,
 * so it follows the same precedent: the link navigates to `NavRoute.Reputation` in-app, not to
 * `BisqLinks.BUILD_REPUTATION_WIKI_URL`. `ReputationScreen` already contains desktop's full
 * "how to build reputation" content (burn BSQ / bond BSQ / signed account age / account age —
 * `reputation.buildReputation.*` keys), so nothing is lost by staying in-app; it is a strictly
 * better destination than a browser tab for something a decentralized-app user needs to trust.
 *
 * ======================================================================================
 * HOW THE SELLER RE-FINDS THIS AFTER "KEEP" — CARD BADGE, NOT A BANNER (DECIDED 2026-09-23)
 * ======================================================================================
 * Two alternatives were weighed and rejected before landing on the card badge:
 *   (a) A persistent offerbook-wide banner. Rejected: the offerbook is a busy, per-market,
 *       per-direction screen (`DirectionToggle` + market selector); a banner would either have
 *       to reappear on every market/direction combination the affected offers are NOT currently
 *       showing in (confusing — "why does this banner exist here on the Buy tab of a market I
 *       have no sell offers in") or be scoped to only the Sell tab of specific markets (fragile
 *       to build, easy to miss when scrolled past the `DirectionToggle`). Desktop has no
 *       persistent banner either — only the popup — so building one here would be new surface
 *       area the issue never asked for.
 *   (b) Rely on per-session dismissal alone, nothing persists on screen. Rejected: the issue's
 *       whole premise is that a seller currently has ZERO way to notice this between the
 *       (rare) trigger moments; dismissing the dialog and having literally nothing left behind
 *       reproduces exactly the "never learns why" problem for the rest of that session — a
 *       buyer could still be hitting the offer, the seller still has no way to re-check short of
 *       waiting for the next score-change trigger.
 *
 * **Decision: a small badge on the offer's own [OfferCard], visible whenever the seller opens
 * "My offers only"** (`OfferbookFilterController`'s existing toggle — see `project_ui_patterns`
 * memory, `onlyMyOffers` / `_onlyMyOffers`) is the SOLE post-"Keep" rediscovery path — no banner,
 * no other surface. `OfferCard.kt` already gives every own-offer card a distinct treatment
 * (`myOfferBackgroundColor`, `directionalLabel` in `myOfferColor`, a bottom-right
 * [RemoveOfferIcon] as the existing delete affordance) — [OffendingOfferCardBadge] is one more
 * icon in that same bottom-right row, reusing `WarningIconLightGrey` (legible on the card's
 * existing translucent primary-tinted background, unlike a saturated `.warning` orange which
 * would compete with [RemoveOfferIcon] for attention in the same corner). Icon-only, no text —
 * matching [RemoveOfferIcon]'s own icon-only precedent on this card, so no new i18n string is
 * needed for it. This is cheap (the card already computes everything needed — `isMyOffer` is
 * already true, and the same offending-check that feeds the dialog can set a boolean on the
 * model, mirroring exactly how `isInvalidDueToReputation` already does this for BUY offers) and
 * it answers "how do I find this again" with the same screen state the seller would check
 * anyway (their own offers), not a new one to remember. Per-session dismissal on the dialog
 * (does not nag) plus this persistent per-card badge (never loses the information) is the
 * complete answer — no additional offerbook chrome is introduced.
 *
 * ======================================================================================
 * REMOVE FLOW — BULK, USING THE EXISTING SINGLE-OFFER API, RESILIENT TO PARTIAL FAILURE
 * ======================================================================================
 * There is no bulk-delete endpoint; [network.bisq.mobile.data.service.offers.OffersServiceFacade.deleteOffer]
 * takes one offer id. "Remove offers" fires one `deleteOffer` call per row (sequential is fine —
 * this list is realistically 1-4 offers, not a pagination-scale operation). As each call
 * succeeds, that row drops out of [OffersBelowReputationUiState.offendingOffers] live, so the
 * user watches the list shrink rather than staring at a spinner with no feedback. A failed call
 * reuses the EXACT existing single-offer failure surface — the snackbar already shown by
 * `OfferbookPresenter.onConfirmedDeleteOffer`
 * (`mobile.bisqEasy.offerbook.failedToDeleteOffer` / `.unableToDeleteOffer`) — and the row STAYS
 * in the list with a small inline error mark (reusing `ExclamationRedIcon`, the same atom the
 * banned-account PoC uses, since a failed delete is this dialog's one genuinely bad-news state)
 * so retrying is just tapping "Remove offers" again; no separate per-row retry control is needed
 * for a list this short.
 *
 * **Rule (decided 2026-09-23): tapping "Keep" after a partial remove failure still suppresses
 * the dialog for that offer — it is NOT forced into a retry loop.** The dismissal rule (above)
 * is keyed to the offending-id set at the moment "Keep" is tapped, with no separate carve-out
 * for "but one of these just failed to delete" — the row's presence in the set is all that
 * matters, not the history of what was attempted on it. Rationale: "Keep" is an affirmative,
 * general-purpose choice — "leave these offers live for now" — and a failed deletion does not
 * change what the user is choosing; forcing a retry would turn one dialog action into two
 * different behaviors depending on an internal detail (did the delete call succeed) the user
 * has no reason to track. The seller is not left worse off for choosing Keep here: the card
 * badge (previous section) already carries the "this offer is still below your reputation"
 * signal forward regardless of why it is still in that state, so nothing is silently lost —
 * the seller can always come back to "My offers only" and try the delete again whenever they
 * choose, the same as for any other own-offer deletion in this app today.
 *
 * ======================================================================================
 * COPY
 * ======================================================================================
 * Headline and the "Remove offers" / "Learn how to build up reputation" actions reuse desktop's
 * existing, already-translated (all 14 locales) keys verbatim — they carry no claim this PoC
 * needs to change. The existing `.message` key is NOT reused (see "VERDICT" above — it asserts
 * the offers are hidden, which is false on mobile) — replaced by a new, mobile-accurate message
 * that names what actually happens (buyers can still reach and try to take these offers, and
 * will be blocked) and states the count. "Keep" has no existing short, standalone key in this
 * codebase (`action.cancel` = "Cancel" describes aborting an operation, not this affirmative
 * choice — see "DIALOG" above) — one small new key.
 *
 * ======================================================================================
 * ACCESSIBILITY
 * ======================================================================================
 * - Long/localized market codes and amount strings (see `project_ui_patterns` memory on
 *   14-language support; German/Russian run ~30-40% longer) use [AutoResizeText] with
 *   `TextOverflow.Ellipsis` for the market label — the same atom `OfferCard.kt` already uses
 *   for maker usernames — rather than a fixed-size `BisqText`, so a long custom-fiat market code
 *   (e.g. a long non-`mainCurrencies` ticker) shrinks instead of clipping mid-code.
 * - `Modifier.testTag(...)` on the dialog's confirm/dismiss buttons and each offer row, per
 *   repo convention — no `semantics { contentDescription = }` blocks added.
 * - The dialog's own scrollable body (inherited from [ConfirmationDialog]/`BisqDialog`) means a
 *   long offending list does not push the sticky Remove/Keep buttons off-screen — verified by
 *   the "several offers" preview below with 5 rows including long market names.
 *
 * ======================================================================================
 * CONNECT BUILDS
 * ======================================================================================
 * Every dependency this needs — `offersByAuthor`, `deleteOffer`, `reputationServiceFacade` —
 * already exists identically on `ClientOffersServiceFacade` (Connect) today; unlike the
 * banned-account-data check, this is not a node-only capability. The only Connect-specific
 * nuance is `AuthorOffersSnapshot.mayBeIncomplete`, already handled above (skip the check on an
 * incomplete pass rather than gating the whole feature behind a capability probe).
 *
 * ======================================================================================
 * PROPOSED I18N KEYS (English base only, per repo convention — nothing added to
 * mobile.properties by this PoC; production implementation adds these)
 * ======================================================================================
 * New (2):
 *   mobile.bisqEasy.offerbook.offersBelowReputation.dialog.message
 *     → "Your reputation score no longer covers the amount on {0} of your sell offers. Buyers
 *        can still see and try to take them, but will be blocked until you remove them or
 *        build up your reputation."
 *   mobile.bisqEasy.offerbook.offersBelowReputation.dialog.keep
 *     → "Keep"
 *   mobile.bisqEasy.offerbook.offersBelowReputation.badge.contentDescription
 *     → "Offer exceeds your reputation limit"
 *        (screen-reader text for [OffendingOfferCardBadge]; the shared icon composable only
 *        carries a fixed "Warning icon" description, which says nothing about why the badge is
 *        there. Production either wraps the icon in `semantics { contentDescription = ... }`
 *        with this key or adds a description parameter to `WarningIconLightGrey`. Not done in
 *        this PoC, whose strings are all hard-coded English pending the keys above.)
 *
 * Reused, already present (all 14 locales) in
 * `shared/domain/.../resources/mobile/bisq_easy.properties`:
 *   bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline
 *     ("Your offer(s) cannot be accepted" — dialog headline)
 *   bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.removeOffers
 *     ("Remove my invalid offers" — confirm button)
 *   bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.buildReputation
 *     ("Learn how to build up reputation" — tertiary link)
 * Reused from `shared/domain/.../resources/mobile/mobile.properties` (failure snackbar, already
 * wired by `OfferbookPresenter.onConfirmedDeleteOffer`):
 *   mobile.bisqEasy.offerbook.failedToDeleteOffer / mobile.bisqEasy.offerbook.unableToDeleteOffer
 *
 * ======================================================================================
 * IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ======================================================================================
 * - Add `isOffendingDueToReputation: Boolean` to `OfferItemPresentationModel`, sibling to
 *   `isInvalidDueToReputation`, computed only for `isMyOffer && direction == SELL` — feeds
 *   [OffendingOfferCardBadge] in `OfferCard.kt`.
 * - `OfferbookPresenter`: add `_showOffersBelowReputationDialog`, `_offendingOffers`,
 *   `_isRemovingOffendingOffers`; a `checkOffersBelowReputation()` suspend fun run from
 *   `onViewAttached()` and from a `reputationServiceFacade.scoreByUserProfileId` collector
 *   (presenterScope, filtered to own id, `distinctUntilChanged()`); a `lastDismissedOfferIds:
 *   Set<String>` field (session-lifetime, not persisted) implementing the subset rule above.
 * - `onRemoveOffendingOffers()`: set `_isRemovingOffendingOffers`, then sequential
 *   `deleteOffer` per row, updating `_offendingOffers`/badges live; failures reuse the existing
 *   snackbar strings and leave the row in place. When the loop ends, clear
 *   `_isRemovingOffendingOffers` in every case, and if no row is left hide the dialog and clear
 *   `_offendingOffers`; if rows failed, the dialog stays open with the failed rows for Keep or
 *   retry.
 * - `onKeepOffendingOffers()`: `lastDismissedOfferIds = current offending id set`; hide dialog.
 * - `onBuildReputationFromOffendingDialog()`: `navigateTo(NavRoute.Reputation)`; hide dialog
 *   (mirrors `onNavigateToReputation` exactly).
 * - Presenter test: fake an offending SELL offer, assert dialog shows on attach and on a
 *   simulated score-change emission; assert Keep suppresses an identical re-check but not one
 *   with an added id; assert Remove calls `deleteOffer` once per row and clears the list on
 *   success.
 * - No `TODO()` calls in this file; no Koin injection; no `IXxxPresenter` interface.
 */
package network.bisq.mobile.presentation.design.offerbook_reputation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.RemoveOfferIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIconLightGrey
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// MVIP sketch (design package only — production shape lives in OfferbookPresenter)
// -------------------------------------------------------------------------------------

/** One offending row: [offerId] is the delete key, the rest is already-formatted display text. */
internal data class OffendingOfferRow(
    val offerId: String,
    val marketLabel: String,
    val amountLabel: String,
    val hasRemoveError: Boolean = false,
)

/**
 * Stands in for the slice of `OfferbookPresenter` state this design touches. Everything else on
 * the real screen (market/direction filters, the unrelated not-enough-reputation-to-TAKE dialog)
 * is unchanged and not modeled here.
 */
internal data class OffersBelowReputationUiState(
    val offendingOffers: List<OffendingOfferRow>,
    val isDialogVisible: Boolean,
    val isRemoving: Boolean,
)

internal sealed interface OffersBelowReputationUiAction {
    data object RemoveOffers : OffersBelowReputationUiAction

    data object Keep : OffersBelowReputationUiAction

    data object BuildReputation : OffersBelowReputationUiAction
}

/** Builds a [OffendingOfferRow] from primitives with realistic defaults. */
internal fun simulatedOffendingOfferRow(
    offerId: String = "off-1",
    marketLabel: String = "BTC/EUR",
    amountLabel: String = "50.00 – 200.00 EUR",
    hasRemoveError: Boolean = false,
): OffendingOfferRow =
    OffendingOfferRow(
        offerId = offerId,
        marketLabel = marketLabel,
        amountLabel = amountLabel,
        hasRemoveError = hasRemoveError,
    )

/** Builds a [OffersBelowReputationUiState] from primitives with realistic defaults. */
internal fun simulatedOffersBelowReputationUiState(
    offendingOffers: List<OffendingOfferRow> = listOf(simulatedOffendingOfferRow()),
    isDialogVisible: Boolean = true,
    isRemoving: Boolean = false,
): OffersBelowReputationUiState =
    OffersBelowReputationUiState(
        offendingOffers = offendingOffers,
        isDialogVisible = isDialogVisible,
        isRemoving = isRemoving,
    )

// -------------------------------------------------------------------------------------
// Dialog — proactive, on offerbook entry / score change
// -------------------------------------------------------------------------------------

/**
 * Blocking-by-default (but NOT non-dismissible — see file KDoc "DIALOG") warning listing every
 * own SELL offer whose amount now exceeds what [uiState]'s current reputation score allows.
 * Two-button contract matching every other [ConfirmationDialog] on this screen: confirm =
 * "Remove offers", dismiss = "Keep". The "Learn how to build up reputation" action is a tertiary
 * underlined link inside the body, not a third button.
 */
@Composable
internal fun OffersBelowReputationDialog(
    uiState: OffersBelowReputationUiState,
    onAction: (OffersBelowReputationUiAction) -> Unit,
    headline: String = "Your offer(s) cannot be accepted",
    message: String = defaultOffersBelowReputationMessage(uiState.offendingOffers.size),
    removeButtonText: String = "Remove my invalid offers",
    keepButtonText: String = "Keep",
    buildReputationText: String = "Learn how to build up reputation",
) {
    ConfirmationDialog(
        headline = headline,
        headlineColor = BisqTheme.colors.warning,
        headlineLeftIcon = { WarningIcon() },
        message = message,
        confirmButtonText = removeButtonText,
        dismissButtonText = keepButtonText,
        confirmButtonLoading = uiState.isRemoving,
        onConfirm = { onAction(OffersBelowReputationUiAction.RemoveOffers) },
        onDismiss = { onAction(OffersBelowReputationUiAction.Keep) },
        extraContent = {
            Column {
                uiState.offendingOffers.forEach { row ->
                    OffendingOfferListRow(row)
                    BisqGap.VHalf()
                }
                BisqGap.VHalf()
                BisqButton(
                    text = buildReputationText,
                    type = BisqButtonType.Underline,
                    onClick = { onAction(OffersBelowReputationUiAction.BuildReputation) },
                    modifier = Modifier.testTag("offers_below_reputation_build_reputation_link"),
                )
            }
        },
    )
}

/**
 * Proposed `mobile.bisqEasy.offerbook.offersBelowReputation.dialog.message`, `{0}` = offer
 * count. Deliberately does NOT claim the offers are hidden from other users — see file KDoc
 * "VERDICT": on mobile they are not.
 */
private fun defaultOffersBelowReputationMessage(offerCount: Int): String =
    "Your reputation score no longer covers the amount on $offerCount of your sell offers. " +
        "Buyers can still see and try to take them, but will be blocked until you remove them " +
        "or build up your reputation."

/** One row: market pair + amount, with an inline error mark on a failed removal attempt. */
@Composable
private fun OffendingOfferListRow(row: OffendingOfferRow) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("offending_offer_row_${row.offerId}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AutoResizeText(
                text = row.marketLabel,
                color = BisqTheme.colors.white,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            BisqText.SmallLight(
                text = row.amountLabel,
                color = BisqTheme.colors.mid_grey20,
            )
        }
        if (row.hasRemoveError) {
            BisqGap.H1()
            ExclamationRedIcon()
        }
    }
}

// -------------------------------------------------------------------------------------
// Card badge — how the seller re-finds this after "Keep" (see file KDoc)
// -------------------------------------------------------------------------------------

/**
 * Persistent per-card indicator for an own SELL offer that currently exceeds the seller's
 * reputation-based limit. Sits alongside [RemoveOfferIcon] in `OfferCard.kt`'s existing
 * bottom-right row for own offers — icon-only, matching [RemoveOfferIcon]'s own precedent. The
 * only string it needs is the screen-reader description listed under "PROPOSED I18N KEYS", since
 * the icon's built-in "Warning icon" text does not say what the badge means. This is the answer
 * to "how does the seller re-find this after Keep" — see file KDoc "HOW THE SELLER RE-FINDS THIS".
 */
@Composable
internal fun OffendingOfferCardBadge() {
    WarningIconLightGrey(modifier = Modifier.testTag("offending_offer_card_badge"))
}

/** Standalone re-creation of `OfferCard`'s own bottom-right row, for badge-in-context previews. */
@Composable
private fun SimulatedOfferCardBottomRow(isOffending: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.SmallLight("SEPA, Bank transfer", color = BisqTheme.colors.mid_grey30)
        Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
            if (isOffending) {
                OffendingOfferCardBadge()
            }
            RemoveOfferIcon()
        }
    }
}

/**
 * Small non-zero host content for a dialog-only preview — see "1. Single offending offer" below
 * for why this is needed. Mirrors the banned-account design PoC's own standalone-dialog preview.
 */
@Composable
private fun DialogPreviewHost(label: String) {
    // Full width, or the preview root shrinks to this label and the dialog window is clipped.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/**
 * 1. Single offending offer — the common case.
 *
 * A [ConfirmationDialog] is a real `Dialog`/`Popup` window; on its own, with no other content in
 * the composition, it gives the preview a zero-size root and Android Studio's renderer throws
 * instead of showing it — the exact failure mode this file's dialog previews hit before this
 * fix. [DialogPreviewHost] is the same small host-column trick the banned-account design PoC
 * (`trade_banned_account/BuyerState2aBannedAccountDesign.kt`) already established for its own
 * standalone-dialog preview: real, non-zero content sits in the composition alongside the
 * dialog so the renderer has something to measure.
 */
@ExcludeFromCoverage
@Preview(name = "1. Dialog — single offer", heightDp = 700)
@Composable
private fun OffersBelowReputation_SingleOffer_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Single offending sell offer:")
        OffersBelowReputationDialog(
            uiState = simulatedOffersBelowReputationUiState(),
            onAction = {},
        )
    }
}

/**
 * 2. Several offending offers, including a long non-`mainCurrencies` market code and a large
 * range amount, to check truncation/i18n wrapping (see file KDoc "ACCESSIBILITY").
 */
@ExcludeFromCoverage
@Preview(name = "2. Dialog — several offers, long market + large amount", heightDp = 900)
@Composable
private fun OffersBelowReputation_SeveralOffers_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Several offending sell offers:")
        OffersBelowReputationDialog(
            uiState =
                simulatedOffersBelowReputationUiState(
                    offendingOffers =
                        listOf(
                            simulatedOffendingOfferRow(offerId = "off-1", marketLabel = "BTC/EUR", amountLabel = "50.00 – 200.00 EUR"),
                            simulatedOffendingOfferRow(offerId = "off-2", marketLabel = "BTC/GBP", amountLabel = "45.00 GBP"),
                            simulatedOffendingOfferRow(
                                offerId = "off-3",
                                marketLabel = "BTC/XAAAAAAAAAAA",
                                amountLabel = "1,250,000.00 XAAAAAAAAAAA",
                            ),
                            simulatedOffendingOfferRow(offerId = "off-4", marketLabel = "BTC/USD", amountLabel = "12.50 – 99.99 USD"),
                            simulatedOffendingOfferRow(offerId = "off-5", marketLabel = "BTC/NGN", amountLabel = "980,000.00 NGN"),
                        ),
                ),
            onAction = {},
        )
    }
}

/** 3. Removing in progress — confirm button shows the loading state, dismiss disabled with it. */
@ExcludeFromCoverage
@Preview(name = "3. Dialog — removing in progress", heightDp = 700)
@Composable
private fun OffersBelowReputation_Removing_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Removing in progress:")
        OffersBelowReputationDialog(
            uiState =
                simulatedOffersBelowReputationUiState(
                    offendingOffers =
                        listOf(
                            simulatedOffendingOfferRow(offerId = "off-1"),
                            simulatedOffendingOfferRow(offerId = "off-2", marketLabel = "BTC/GBP", amountLabel = "45.00 GBP"),
                        ),
                    isRemoving = true,
                ),
            onAction = {},
        )
    }
}

/**
 * 4. Remove failure — one row failed to delete and stays listed with an inline error mark; the
 * other succeeded and already dropped out of the list (see file KDoc "REMOVE FLOW").
 */
@ExcludeFromCoverage
@Preview(name = "4. Dialog — remove failure, one row left with error", heightDp = 700)
@Composable
private fun OffersBelowReputation_RemoveFailure_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Remove failed for one offer:")
        OffersBelowReputationDialog(
            uiState =
                simulatedOffersBelowReputationUiState(
                    offendingOffers =
                        listOf(
                            simulatedOffendingOfferRow(offerId = "off-2", marketLabel = "BTC/GBP", amountLabel = "45.00 GBP", hasRemoveError = true),
                        ),
                ),
            onAction = {},
        )
    }
}

/**
 * 5. Card badge in context — "Keep" was tapped, the dialog is gone, but the offer's own card
 * (as seen via the existing "My offers only" filter) still marks it. Left card: fine. Right
 * card: offending. Answers "how does the seller re-find this after Keep."
 */
@ExcludeFromCoverage
@Preview(name = "5. Card badge — how the seller re-finds this after Keep")
@Composable
private fun OffersBelowReputation_CardBadgeInContext_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallLight(
                "Own offer card, healthy:",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.VHalf()
            SimulatedOfferCardBottomRow(isOffending = false)
            BisqGap.V2()
            BisqText.SmallLight(
                "Own offer card, below reputation (after Keep, badge persists):",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.VHalf()
            SimulatedOfferCardBottomRow(isOffending = true)
        }
    }
}

/** 6. Long-locale simulation — German runs ~30-40% longer than English (see file KDoc). */
@ExcludeFromCoverage
@Preview(name = "6. Dialog — simulated long-locale text", heightDp = 750)
@Composable
private fun OffersBelowReputation_LongLocaleText_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Simulated German locale:")
        OffersBelowReputationDialog(
            uiState = simulatedOffersBelowReputationUiState(),
            onAction = {},
            headline = "Ihr(e) Angebot(e) kann/können nicht angenommen werden",
            message =
                "Ihr Reputationsscore deckt den Betrag für 1 Ihrer Verkaufsangebote nicht mehr ab. " +
                    "Käufer können sie weiterhin sehen und versuchen anzunehmen, werden aber " +
                    "blockiert, bis Sie sie entfernen oder Ihre Reputation aufbauen.",
            removeButtonText = "Meine ungültigen Angebote entfernen",
            keepButtonText = "Behalten",
            buildReputationText = "Erfahren Sie, wie Sie Ihre Reputation aufbauen können",
        )
    }
}
