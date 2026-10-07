/**
 * OffersBelowReputationDesign.kt
 *
 * Compose specification for warning a seller when one or more of their own sell offers no longer
 * meet their current reputation-based amount limit. Not wired to a presenter; state shown in the
 * previews below is produced entirely by the `simulatedXxx` helpers, which take only primitives.
 *
 * Where the #1873 implementation differs, the code wins: other users do not see the offers (see
 * "3."), a failed remove has its own `offersBelowReputation.removeFailed` keys instead of the
 * single-offer delete ones ("8.", "10."), tests find elements by text and content description
 * rather than test tags ("11."), and "Go to market" turns "only my offers" on without saving it
 * for that market (`showOnlyMyOffersOnArrival`, not `setOnlyMyOffers`; "8.", "13.").
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE
 * ------------------------------------------------------------------------------------
 * A seller's reputation score can drop after an offer is published (profile-age recalculation,
 * bond/burn expiry, ranking changes). A sell offer whose amount the current score no longer
 * covers stays live in the offerbook; a buyer who tries to take it is stopped by their own
 * not-enough-reputation dialog (`TakeOfferCoordinator.checkTakeOfferEligibility`, SELL branch),
 * but the seller who published it gets no signal at all. This specification adds an app-wide
 * warning banner plus a review dialog so a seller learns about this without having to happen to
 * revisit the offerbook.
 *
 * ------------------------------------------------------------------------------------
 * 2. DATA SOURCE AND DETECTION
 * ------------------------------------------------------------------------------------
 * Formula: `withTolerance(sellersScore) < requiredReputationScoreForMinOrFixed`, where the
 * required score is resolved by
 * [network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount]
 * and the tolerance by
 * [network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.withTolerance] — both already exist
 * in `shared/domain/.../domain/utils/BisqEasyTradeAmountLimits.kt`, ported from Desktop's own
 * `BisqEasySellersReputationBasedTradeAmountService`.
 *
 * Fetch source: `OffersServiceFacade.offersByAuthor` — every market, not the currently selected
 * one, matching Desktop's own all-channels scan. Its `mayBeIncomplete` flag (true while the
 * all-markets offers cache is still syncing over a Tor connection) gates the check: a pass made
 * while `mayBeIncomplete` is true is skipped rather than treated as "no offending offers," and
 * retried on the same interval and retry budget `PeerProfilePresenter.loadPeerOffers` already
 * uses (`PEER_OFFERS_SYNC_RETRY_MS` / `PEER_OFFERS_SYNC_RETRIES`) — the query is a local cache
 * read, not a round trip, so the retry adds no network cost.
 *
 * Detection is an app-wide use case producing `StateFlow<List<InvalidOwnOfferRow>>`, consumed by
 * `TabContainerPresenter` rather than owned by `OfferbookPresenter`. Two triggers feed it: the
 * presenter's own attach (`TabContainerPresenter` is alive for the app's whole tab-shell session,
 * so this replaces the previous "on offerbook screen entry" trigger with "for as long as the app
 * is running"), and a live collector on `reputationServiceFacade.scoreByUserProfileId`, filtered
 * to the local profile id and `distinctUntilChanged()`, recomputing on every change while the
 * process stays alive. Every dependency — `offersByAuthor`, `deleteOffer`, `reputationServiceFacade`,
 * `marketPriceServiceFacade`, `configServiceFacade.tradeAmountLimits` — already exists identically
 * on `ClientOffersServiceFacade` (Connect), so this is not a node-only capability; the only added
 * cost of running it app-wide instead of only while the offerbook screen is attached is one more
 * long-lived `StateFlow` collector for the session, not new computation or a new network
 * dependency — the underlying reads were already cheap local cache reads.
 *
 * `OfferbookPresenter` no longer runs this check itself. For the card badge (see "9. Card badge"),
 * it only needs to know whether the offer id it is currently rendering appears in the shared
 * `StateFlow<List<InvalidOwnOfferRow>>` — a membership check against state it already collects,
 * not a second computation of the reputation comparison.
 *
 * ------------------------------------------------------------------------------------
 * 3. WHETHER OFFENDING OFFERS ARE HIDDEN FROM OTHER USERS: THEY ARE (SUPERSEDED, SEE BELOW)
 * ------------------------------------------------------------------------------------
 * Superseded by the #1873 implementation, by maintainer decision: mobile hides them as Desktop
 * does. Bisq Node applies Bisq's `isValidOfferbookMessage` rule, and Bisq Connect filters other
 * makers' below-reputation sell offers client-side with the same rule; the maker still sees
 * their own. The dialog therefore reuses Desktop's
 * `bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.message` instead of
 * the `dialog.message` key proposed in "10. Proposed i18n keys." The analysis below is kept as
 * it was written.
 *
 * Desktop's own popup states that offending offers are hidden from other users, because on
 * Desktop `hasSellerSufficientReputation` also gates the offerbook list itself for every offer
 * that is not the viewer's own. Mobile has no equivalent filter: grepping the whole `shared/`
 * module for `SufficientReputation` / `ReputationBasedTradeAmount` outside generated translation
 * bundles returns nothing. `OfferbookPresenter.processOffer` only computes
 * `OfferItemPresentationModel.isInvalidDueToReputation` for `DirectionEnum.BUY` offers (checking
 * whether the viewer, as a prospective seller/taker, clears the requirement) — it never evaluates
 * a SELL offer's own maker score, and it never removes anything from `sortedFilteredOffers`. A
 * seller's below-reputation SELL offer therefore stays fully visible to every buyer on mobile, who
 * can still reach it and attempt to take it, and is stopped only by their own
 * `TakeOfferEligibility.NotEnoughReputation` result when `checkTakeOfferEligibility` resolves the
 * maker's score. The banner and dialog copy states this plainly rather than claiming the offers
 * are hidden — see "10. Proposed i18n keys."
 *
 * ------------------------------------------------------------------------------------
 * 4. VISIBILITY SCOPE AND PLACEMENT
 * ------------------------------------------------------------------------------------
 * [InvalidOwnOffersBanner] renders inside `TabContainerScreen.kt`'s `BisqStaticScaffold` `content`
 * slot, above `TabNavGraph(tabNavController)` and below the scaffold's own `topBar`, so it is
 * visible on every one of the four tabs (Dashboard, Offerbook, My Trades, More) without being part
 * of any one tab's own screen content. It is not shown while navigation has moved outside the tab
 * shell (a trade detail screen, the create-offer wizard, chat) — those are reached through
 * `rootNavController`, not `tabNavController`, so the banner's slot is simply not in that part of
 * the composition tree; nothing has to explicitly hide it there.
 *
 * `AlertNotificationBanner` (`shared/presentation/.../common/ui/alert/banner/
 * AlertNotificationBanner.kt`) is not inside `TabContainerScreen.kt` — it renders at the
 * application root, in `App.kt`'s `Column { NetworkStatusBanner(); AlertNotificationBanner(...);
 * navGraphContent() }`, above the entire navigation graph, which includes `TabContainerScreen`.
 * The two banners therefore already stack in a fixed order purely as a consequence of where each
 * one sits in the composition tree: `NetworkStatusBanner`, then `AlertNotificationBanner`, then
 * (inside the nav graph) `TabContainerScreen`'s own top bar, then [InvalidOwnOffersBanner], then
 * tab content. No shared queue or priority comparison between the two banners is needed to produce
 * that order — see "6. Stacking with the alert banner."
 *
 * ------------------------------------------------------------------------------------
 * 5. BANNER ANATOMY
 * ------------------------------------------------------------------------------------
 * [InvalidOwnOffersBanner] is one line of text, `BisqTheme.colors.warning` tint — not `danger` —
 * because this is a self-diagnostic condition the seller caused and can resolve themselves
 * (their own score changed, or they can remove the offers), not a signed, network-wide,
 * authoritative alert the way the security-manager banner is; reserving `danger` for that
 * distinction keeps the color meaning consistent with how the rest of the app already uses it
 * (`AlertNotificationCommonUi.alertAccentColor` maps `AlertType.EMERGENCY` to `danger`, not
 * `WARN`). The whole banner is one tap target, opening [InvalidOwnOffersReviewDialog]; the
 * trailing dismiss icon is a separate, smaller tap target so a seller aiming for "read more" does
 * not accidentally dismiss instead. The banner is not shown at all while
 * [InvalidOwnOffersUiState.offendingOffers] is empty or while the current dismissal covers
 * every id currently in that list — see "7. Dismissal rule."
 *
 * ------------------------------------------------------------------------------------
 * 6. STACKING WITH THE ALERT BANNER
 * ------------------------------------------------------------------------------------
 * The two banners are independent composables with independent dismiss state and independent data
 * sources (a signed security-manager alert vs. a locally-derived reputation comparison) — they are
 * not merged into `AlertNotificationBannerPresenter`'s own alert queue (which already has its own
 * "+N more" mechanism for multiple alerts of that one kind, `PendingAlertsCounter`). Folding a
 * structurally different kind of banner into that queue would mix two different severities and
 * two different dismiss semantics behind one counter. The alert banner keeps visual precedence:
 * it always renders above [InvalidOwnOffersBanner] when both are visible, which — per
 * "4. Visibility scope and placement" — falls out of where each composable already sits in the
 * tree rather than needing new coordination logic between the two presenters. At most these two
 * banners stack; there is no scenario in this app today where more than two independent banner
 * sources compete for the same vertical slot.
 *
 * ------------------------------------------------------------------------------------
 * 7. DISMISSAL RULE
 * ------------------------------------------------------------------------------------
 * Dismissal is scoped to the process lifetime (in-memory state, not persisted), cleared on the
 * next cold start regardless of how long that takes — not on backgrounding/foregrounding, which
 * happens far more often on mobile (switching to check a message, an incoming call) and would
 * reproduce, on every such switch, the exact nagging the dismissal exists to prevent.
 *
 * Dismissal is keyed to the exact SET of offending offer ids present at the moment of dismissal
 * (a sorted id list, no hash needed), not a single boolean: on the next detection pass, if the
 * freshly computed offending set is a subset of (or equal to) the last-dismissed set, the banner
 * stays hidden; if it contains any id not in the last-dismissed set, the banner shows again with
 * the full current list, not just the newly added offers — matching how the review dialog always
 * lists everything currently wrong. A single boolean would also suppress a genuinely new problem
 * (a further score drop invalidating one more offer, or a newly created offer starting out
 * invalid) for the rest of the process lifetime, which is a different thing than "don't repeat
 * the same warning." Removing an offer or the score recovering only shrinks the offending set,
 * which is itself always a subset of anything previously dismissed, so resolved offers never
 * re-trigger the banner. Both dismiss entry points — the banner's own X icon and the review
 * dialog's "Keep" action — write to the same dismissed-id-set; they are two places to reach one
 * rule, not two different rules.
 *
 * The dismissed-id-set and the app-wide `StateFlow<List<InvalidOwnOfferRow>>` both live in the
 * session-scoped use case/service (a Koin `single` in the domain layer), not in
 * `TabContainerPresenter`. `TabContainerPresenter` is registered as a Koin `factory` and attached
 * through `RememberPresenterLifecycle`, so a new instance is created every time the tab shell
 * leaves composition and returns (any push to a non-tab route — offerbook, a trade, settings —
 * and back); a presenter-held set would be cleared far more often than a cold start, defeating the
 * "reappears only on the next cold start" half of this rule. A Koin `single`'s plain in-memory
 * state is cleared on cold start by construction (the process that held it no longer exists) and
 * survives everything short of that, including the presenter recreations the tab shell's own
 * navigation already causes. Banner visibility is computed inside the `single` itself —
 * `invalid.isNotEmpty() && !dismissed.containsAll(invalid.map { it.offerId })` — not in the
 * presenter; the presenter only reads the resulting `StateFlow` and forwards dispatched actions to
 * the `single`'s dismiss/remove functions.
 *
 * ------------------------------------------------------------------------------------
 * 8. REVIEW DIALOG
 * ------------------------------------------------------------------------------------
 * [InvalidOwnOffersReviewDialog] reuses
 * [network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog]'s
 * `extraContent` slot to render the offending-offer rows between the message and the sticky
 * buttons, the same way every other variable-length `ConfirmationDialog` body on this screen
 * already does. Two-button contract: confirm = "Remove offers" (not styled `.danger` — deleting
 * one's own offer is a normal, reversible-by-recreating action, not fraud-tier severity), dismiss
 * = "Keep" (an affirmative choice to leave the offers as they are, distinct from "Cancel," which
 * would describe aborting an operation rather than choosing to keep something). "Learn how to
 * build up reputation" is a tertiary underlined link inside `extraContent`, below the offer list
 * and above the sticky buttons, so the two-button contract is not broken by a third full-width
 * button; tapping it navigates to `NavRoute.Reputation` in-app — the same destination
 * `OfferbookPresenter` already navigates to for the symmetric "my own score is short" case on the
 * take-offer side — rather than an external wiki page.
 *
 * Each row is tappable and dispatches `OnGoToMarket(offerId, marketCode)`, selecting that market
 * (`OffersServiceFacade.selectOfferbookMarket`) and navigating to the market's offerbook with the
 * "only my offers" filter already on, then closing the dialog. `NavRoute.Offerbook` is currently a
 * plain, parameterless object; it needs an optional `onlyMyOffers: Boolean = false` parameter so
 * the destination screen can enable that filter (`OfferbookPresenter.setOnlyMyOffers`) on arrival
 * instead of only in response to a manual toggle — see "13. Implementation notes."
 *
 * Removing is bulk, using the existing single-offer `OffersServiceFacade.deleteOffer` call once
 * per row (sequential; the list is realistically 1-4 offers, not pagination scale). As each call
 * succeeds, that row drops out of the list live; a failed call reuses the existing single-offer
 * failure snackbar (`mobile.bisqEasy.offerbook.failedToDeleteOffer` /
 * `.unableToDeleteOffer`) and the row stays with an inline error mark, so retrying is just tapping
 * "Remove offers" again. Tapping "Keep" after a partial failure still applies the dismissal rule
 * to the current offending set, including any row that just failed to remove — "Keep" is a
 * general "leave these as they are for now" choice, not conditioned on which specific delete
 * calls happened to succeed; the card badge (see "9. Card badge") keeps carrying the signal for
 * that offer regardless.
 *
 * ------------------------------------------------------------------------------------
 * 9. CARD BADGE
 * ------------------------------------------------------------------------------------
 * [OffendingOfferCardBadge] stays as a secondary re-find affordance on the offer's own card,
 * visible whenever the seller opens "My offers only" in a market's offerbook. It answers "how do
 * I find this again after dismissing the banner for the session" with the same screen state the
 * seller would check anyway, and it is the one trace of the condition that survives a session
 * dismissal — the banner is gone until the next cold start or the next new offense, the badge is
 * not. It is icon-only, matching the existing delete affordance in the same bottom-right row on an
 * own-offer card, so no additional string is needed for the badge itself beyond its accessibility
 * description.
 *
 * ------------------------------------------------------------------------------------
 * 10. PROPOSED I18N KEYS
 * ------------------------------------------------------------------------------------
 * English base values only, per repo convention — nothing is added to `mobile.properties` by this
 * file; production implementation adds these.
 *
 * New (`dialog.message` was dropped in favour of Desktop's message, see "3."):
 *   mobile.bisqEasy.offerbook.offersBelowReputation.dialog.message = "Your reputation score no
 *     longer covers the amount on {0} of your sell offers. Buyers can still see and try to take
 *     them, but will be blocked until you remove them or build up your reputation."
 *   mobile.bisqEasy.offerbook.offersBelowReputation.dialog.keep = "Keep"
 *   mobile.bisqEasy.offerbook.offersBelowReputation.dialog.goToMarket = "Go to market"
 *   mobile.bisqEasy.offerbook.offersBelowReputation.badge.contentDescription = "Offer exceeds your
 *     reputation limit"
 *
 * Reused, already present (all 14 locales) in `shared/domain/.../resources/mobile/
 * bisq_easy.properties`:
 *   bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline = "Your
 *     offer(s) cannot be accepted" — dialog headline AND the banner's one-line text, reused
 *     verbatim rather than drafting separate banner copy.
 *   bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.removeOffers =
 *     "Remove my invalid offers"
 *   bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.buildReputation =
 *     "Learn how to build up reputation"
 * Reused from `shared/domain/.../resources/mobile/mobile.properties`:
 *   mobile.alert.actions.dismiss.label = "Dismiss" (banner dismiss icon content description)
 *   mobile.bisqEasy.offerbook.failedToDeleteOffer / mobile.bisqEasy.offerbook.unableToDeleteOffer
 *     (failure snackbar, already wired by the existing single-offer delete path)
 *
 * ------------------------------------------------------------------------------------
 * 11. ACCESSIBILITY
 * ------------------------------------------------------------------------------------
 * - Long/localized market codes and amount strings use [AutoResizeText] with `TextOverflow
 *   .Ellipsis` for the market label, the same atom `OfferCard.kt` already uses for maker
 *   usernames, rather than a fixed-size `BisqText`, so a long custom-fiat market code shrinks
 *   instead of clipping mid-code — see [InvalidOwnOffersReviewDialog_LongLocaleText_Preview].
 * - `Modifier.testTag(...)` marks the banner, its dismiss icon, each row, and the dialog's
 *   confirm/dismiss/keep controls, per repo convention — no `semantics {}` block is used as a
 *   test-id substitute.
 * - The dialog's own scrollable body means a long offending list does not push the sticky
 *   Remove/Keep buttons off-screen.
 * - The banner's dismiss icon and the row's "go to market" tap target are accessible
 *   independently of each other, with distinct content descriptions, so a screen reader user does
 *   not have to guess which action a single announced "warning" element performs.
 *
 * ------------------------------------------------------------------------------------
 * 12. TESTS TO ADD
 * ------------------------------------------------------------------------------------
 * - Use case test: fake an offending SELL offer; assert the app-wide `StateFlow` emits it on
 *   attach and on a simulated score-change emission; assert an incomplete `offersByAuthor`
 *   snapshot is skipped and retried rather than treated as "no offending offers."
 * - Use case/service test: dismissing suppresses an identical re-check but not one with an added
 *   id; dismissing via the dialog's "Keep" applies the same rule as dismissing via the banner's
 *   icon; the dismissal survives a `TabContainerPresenter` recreation (a new presenter instance
 *   reading the same `single` still sees the banner hidden) with the offending set unchanged; a
 *   new offender id appearing after a dismissal re-shows the banner even without any presenter
 *   recreation.
 * - Presenter test for removal: `OnRemoveOffers` calls `deleteOffer` once per row and clears the
 *   list on success; a partial failure leaves the failed row with its error mark and the dialog
 *   open.
 * - UI test: the banner is not rendered when the offending list is empty or fully dismissed;
 *   tapping a row dispatches `OnGoToMarket` with that row's offer id and market code; tapping
 *   "Remove offers" shows the loading state on the confirm button.
 *
 * ------------------------------------------------------------------------------------
 * 13. IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ------------------------------------------------------------------------------------
 * - New app-wide use case/service registered as a Koin `single` in the domain layer (see "2. Data
 *   source and detection" and "7. Dismissal rule"), owning both the detection `StateFlow<List
 *   <InvalidOwnOfferRow>>` and the dismissed-id-set, and exposing the already-combined banner
 *   visibility plus dismiss/remove functions. `TabContainerPresenter` (Koin `factory`, a new
 *   instance on every tab-shell recomposition — see "7. Dismissal rule") only collects that
 *   `StateFlow` and forwards `OnDismissBanner`/`OnKeep`/`OnRemoveOffers` to it; it holds no
 *   `lastDismissedOfferIds` or offending-list state of its own, unlike `showTradeRestrictedDialog`
 *   / `isCreateOfferEnabled`, which are fine to stay presenter-local because nothing about them
 *   depends on surviving a presenter recreation.
 * - `TabContainerScreen.kt`: render [InvalidOwnOffersBanner] inside the `BisqStaticScaffold`
 *   `content` lambda, above `TabNavGraph(tabNavController)`.
 * - `OfferbookPresenter`: remove its own `checkOffersBelowReputation()` trigger and
 *   `isOffendingDueToReputation` computation; read the shared `StateFlow` for the card-badge
 *   membership check instead — see "2. Data source and detection."
 * - Deep link: turn `NavRoute.Offerbook` from a parameterless `data object` into a `data class`
 *   with an optional `onlyMyOffers: Boolean = false` parameter — no existing mechanism carries a
 *   pre-set filter into that screen. The destination presenter's `onViewAttached()` calls
 *   `setOnlyMyOffers(true)` when it is set — see "8. Review dialog."
 */
package network.bisq.mobile.presentation.design.offerbook_reputation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
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
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CloseIcon
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
// State and actions (production shape: detection and dismissal own their state in a
// session-scoped Koin `single`; `TabContainerPresenter` only reads and forwards — see
// "13. Implementation notes for the developer" above)
// -------------------------------------------------------------------------------------

/** One offending row: [offerId] is the delete/navigation key, the rest is display text. */
internal data class InvalidOwnOfferRow(
    val offerId: String,
    val marketCode: String,
    val marketLabel: String,
    val amountLabel: String,
    val hasRemoveError: Boolean = false,
)

internal data class InvalidOwnOffersUiState(
    val offendingOffers: List<InvalidOwnOfferRow>,
    val isBannerVisible: Boolean,
    val isReviewDialogVisible: Boolean,
    val isRemoving: Boolean,
)

internal sealed interface InvalidOwnOffersUiAction {
    data object OnOpenReviewDialog : InvalidOwnOffersUiAction

    data object OnDismissBanner : InvalidOwnOffersUiAction

    data object OnCloseReviewDialog : InvalidOwnOffersUiAction

    data object OnRemoveOffers : InvalidOwnOffersUiAction

    data object OnKeep : InvalidOwnOffersUiAction

    data object OnBuildReputation : InvalidOwnOffersUiAction

    data class OnGoToMarket(
        val offerId: String,
        val marketCode: String,
    ) : InvalidOwnOffersUiAction
}

/** Builds an [InvalidOwnOfferRow] from primitives with realistic defaults. */
internal fun simulatedInvalidOwnOfferRow(
    offerId: String = "off-1",
    marketCode: String = "BTC/EUR",
    marketLabel: String = "BTC/EUR",
    amountLabel: String = "50.00 - 200.00 EUR",
    hasRemoveError: Boolean = false,
): InvalidOwnOfferRow =
    InvalidOwnOfferRow(
        offerId = offerId,
        marketCode = marketCode,
        marketLabel = marketLabel,
        amountLabel = amountLabel,
        hasRemoveError = hasRemoveError,
    )

/** Builds an [InvalidOwnOffersUiState] from primitives with realistic defaults. */
internal fun simulatedInvalidOwnOffersUiState(
    offendingOffers: List<InvalidOwnOfferRow> = listOf(simulatedInvalidOwnOfferRow()),
    isBannerVisible: Boolean = true,
    isReviewDialogVisible: Boolean = false,
    isRemoving: Boolean = false,
): InvalidOwnOffersUiState =
    InvalidOwnOffersUiState(
        offendingOffers = offendingOffers,
        isBannerVisible = isBannerVisible,
        isReviewDialogVisible = isReviewDialogVisible,
        isRemoving = isRemoving,
    )

// -------------------------------------------------------------------------------------
// Banner — see "4. Visibility scope and placement" and "5. Banner anatomy"
// -------------------------------------------------------------------------------------

@Composable
internal fun InvalidOwnOffersBanner(
    uiState: InvalidOwnOffersUiState,
    onAction: (InvalidOwnOffersUiAction) -> Unit,
    message: String = "Your offer(s) cannot be accepted",
) {
    if (!uiState.isBannerVisible || uiState.offendingOffers.isEmpty()) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.warning.copy(alpha = 0.12f))
                .clickable { onAction(InvalidOwnOffersUiAction.OnOpenReviewDialog) }
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf)
                .testTag("invalid_own_offers_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            WarningIcon()
            BisqText.SmallMedium(
                text = message,
                color = BisqTheme.colors.warning,
            )
        }
        IconButton(
            onClick = { onAction(InvalidOwnOffersUiAction.OnDismissBanner) },
            modifier = Modifier.testTag("invalid_own_offers_banner_dismiss"),
        ) {
            CloseIcon()
        }
    }
}

// -------------------------------------------------------------------------------------
// Review dialog — see "8. Review dialog"
// -------------------------------------------------------------------------------------

@Composable
internal fun InvalidOwnOffersReviewDialog(
    uiState: InvalidOwnOffersUiState,
    onAction: (InvalidOwnOffersUiAction) -> Unit,
    headline: String = "Your offer(s) cannot be accepted",
    message: String = defaultInvalidOwnOffersMessage(uiState.offendingOffers.size),
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
        onConfirm = { onAction(InvalidOwnOffersUiAction.OnRemoveOffers) },
        onDismiss = { onAction(InvalidOwnOffersUiAction.OnKeep) },
        extraContent = {
            Column {
                uiState.offendingOffers.forEach { row ->
                    InvalidOwnOfferListRow(row, onAction)
                    BisqGap.VHalf()
                }
                BisqGap.VHalf()
                BisqButton(
                    text = buildReputationText,
                    type = BisqButtonType.Underline,
                    onClick = { onAction(InvalidOwnOffersUiAction.OnBuildReputation) },
                    modifier = Modifier.testTag("invalid_own_offers_build_reputation_link"),
                )
            }
        },
    )
}

private fun defaultInvalidOwnOffersMessage(offerCount: Int): String =
    "Your reputation score no longer covers the amount on $offerCount of your sell offers. " +
        "Buyers can still see and try to take them, but will be blocked until you remove them " +
        "or build up your reputation."

/** One row: market pair + amount, tappable to jump to that market with "only my offers" on. */
@Composable
private fun InvalidOwnOfferListRow(
    row: InvalidOwnOfferRow,
    onAction: (InvalidOwnOffersUiAction) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onAction(InvalidOwnOffersUiAction.OnGoToMarket(row.offerId, row.marketCode)) }
                .testTag("invalid_own_offer_row_${row.offerId}"),
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
        BisqGap.HHalf()
        BisqText.SmallMedium(
            text = "Go to market",
            color = BisqTheme.colors.primary,
        )
    }
}

// -------------------------------------------------------------------------------------
// Card badge — see "9. Card badge"
// -------------------------------------------------------------------------------------

@Composable
internal fun OffendingOfferCardBadge() {
    WarningIconLightGrey(modifier = Modifier.testTag("offending_offer_card_badge"))
}

/** Standalone reproduction of an own-offer card's own bottom-right row, for badge-in-context previews. */
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

// -------------------------------------------------------------------------------------
// Tab-container context reproduction, for the "in context" previews
// -------------------------------------------------------------------------------------

@Composable
private fun SimulatedTabTopBar() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.dark_grey30)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.BaseRegular(text = "Bisq", color = BisqTheme.colors.white)
    }
}

@Composable
private fun SimulatedAlertBanner(message: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.danger.copy(alpha = 0.15f))
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        ExclamationRedIcon()
        BisqText.SmallMedium(text = message, color = BisqTheme.colors.danger)
    }
}

@Composable
private fun SimulatedDashboardContent() {
    Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
        BisqText.H5Light("Market price")
        BisqGap.V1()
        BisqText.BaseLightGrey("111247.40 BTC/USD")
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** Banner alone, inside the tab-container context, above the dashboard content. */
@ExcludeFromCoverage
@Preview(name = "1. Banner — in the tab container, above the dashboard", heightDp = 500)
@Composable
private fun InvalidOwnOffersBanner_InTabContainer_Preview() {
    BisqTheme.Preview {
        Column {
            SimulatedTabTopBar()
            InvalidOwnOffersBanner(
                uiState = simulatedInvalidOwnOffersUiState(),
                onAction = {},
            )
            SimulatedDashboardContent()
        }
    }
}

/** Banner stacked below a security-manager alert banner — precedence via composition order. */
@ExcludeFromCoverage
@Preview(name = "2. Banner — stacked with an alert banner", heightDp = 550)
@Composable
private fun InvalidOwnOffersBanner_StackedWithAlertBanner_Preview() {
    BisqTheme.Preview {
        Column {
            SimulatedAlertBanner("Trading requires app version 2.1.8 or newer.")
            SimulatedTabTopBar()
            InvalidOwnOffersBanner(
                uiState = simulatedInvalidOwnOffersUiState(),
                onAction = {},
            )
            SimulatedDashboardContent()
        }
    }
}

/** Review dialog — a single offending offer. */
@ExcludeFromCoverage
@Preview(name = "3. Dialog — single offer", heightDp = 700)
@Composable
private fun InvalidOwnOffersReviewDialog_SingleOffer_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Single offending sell offer:")
        InvalidOwnOffersReviewDialog(
            uiState = simulatedInvalidOwnOffersUiState(),
            onAction = {},
        )
    }
}

/** Review dialog — several offending offers, including a long market code and a large amount. */
@ExcludeFromCoverage
@Preview(name = "4. Dialog — several offers", heightDp = 900)
@Composable
private fun InvalidOwnOffersReviewDialog_SeveralOffers_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Several offending sell offers:")
        InvalidOwnOffersReviewDialog(
            uiState =
                simulatedInvalidOwnOffersUiState(
                    offendingOffers =
                        listOf(
                            simulatedInvalidOwnOfferRow(offerId = "off-1", marketCode = "BTC/EUR", marketLabel = "BTC/EUR", amountLabel = "50.00 - 200.00 EUR"),
                            simulatedInvalidOwnOfferRow(offerId = "off-2", marketCode = "BTC/GBP", marketLabel = "BTC/GBP", amountLabel = "45.00 GBP"),
                            simulatedInvalidOwnOfferRow(
                                offerId = "off-3",
                                marketCode = "BTC/XAAAAAAAAAAA",
                                marketLabel = "BTC/XAAAAAAAAAAA",
                                amountLabel = "1,250,000.00 XAAAAAAAAAAA",
                            ),
                            simulatedInvalidOwnOfferRow(offerId = "off-4", marketCode = "BTC/USD", marketLabel = "BTC/USD", amountLabel = "12.50 - 99.99 USD"),
                            simulatedInvalidOwnOfferRow(offerId = "off-5", marketCode = "BTC/NGN", marketLabel = "BTC/NGN", amountLabel = "980,000.00 NGN"),
                        ),
                ),
            onAction = {},
        )
    }
}

/** Dialog — removing in progress: confirm button shows the loading state. */
@ExcludeFromCoverage
@Preview(name = "5. Dialog — removing in progress", heightDp = 700)
@Composable
private fun InvalidOwnOffersReviewDialog_Removing_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Removing in progress:")
        InvalidOwnOffersReviewDialog(
            uiState =
                simulatedInvalidOwnOffersUiState(
                    offendingOffers =
                        listOf(
                            simulatedInvalidOwnOfferRow(offerId = "off-1"),
                            simulatedInvalidOwnOfferRow(offerId = "off-2", marketCode = "BTC/GBP", marketLabel = "BTC/GBP", amountLabel = "45.00 GBP"),
                        ),
                    isRemoving = true,
                ),
            onAction = {},
        )
    }
}

/** Dialog — one row failed to remove and stays listed with an inline error mark. */
@ExcludeFromCoverage
@Preview(name = "6. Dialog — remove failure, one row left with error", heightDp = 700)
@Composable
private fun InvalidOwnOffersReviewDialog_RemoveFailure_Preview() {
    BisqTheme.Preview {
        DialogPreviewHost("Remove failed for one offer:")
        InvalidOwnOffersReviewDialog(
            uiState =
                simulatedInvalidOwnOffersUiState(
                    offendingOffers =
                        listOf(
                            simulatedInvalidOwnOfferRow(offerId = "off-2", marketCode = "BTC/GBP", marketLabel = "BTC/GBP", amountLabel = "45.00 GBP", hasRemoveError = true),
                        ),
                ),
            onAction = {},
        )
    }
}

/**
 * Dismissed state: the banner is gone for the session, the card badge is the only remaining
 * trace on the offer's own card — see "9. Card badge."
 */
@ExcludeFromCoverage
@Preview(name = "7. Dismissed — banner gone, card badge remains")
@Composable
private fun InvalidOwnOffers_DismissedCardBadge_Preview() {
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
                "Own offer card, still below reputation after the banner was dismissed:",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.VHalf()
            SimulatedOfferCardBottomRow(isOffending = true)
        }
    }
}

/** Long-locale simulation — German text runs longer than English. */
@ExcludeFromCoverage
@Preview(name = "8. Banner and dialog — simulated long-locale text", heightDp = 800)
@Composable
private fun InvalidOwnOffersReviewDialog_LongLocaleText_Preview() {
    BisqTheme.Preview {
        Column {
            SimulatedTabTopBar()
            InvalidOwnOffersBanner(
                uiState = simulatedInvalidOwnOffersUiState(),
                onAction = {},
                message = "Ihr(e) Angebot(e) kann/können nicht angenommen werden",
            )
        }
        DialogPreviewHost("Simulated German locale:")
        InvalidOwnOffersReviewDialog(
            uiState = simulatedInvalidOwnOffersUiState(),
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

/**
 * Small non-zero host content for a dialog-only preview: a real `Dialog`/`Popup` window with no
 * other content in the composition gives the preview a zero-size root, so a host column with one
 * line of text sits alongside it.
 */
@Composable
private fun DialogPreviewHost(label: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
    }
}
