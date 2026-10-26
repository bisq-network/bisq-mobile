package network.bisq.mobile.presentation.design.trade_terms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Maker's trade terms: one shared block for the take-offer review step and the peer profile.
 *
 * Design preview file. Open it in Android Studio's Split / Design view and review the
 * `@Preview` functions. Nothing here is wired to a presenter, Koin or a domain type.
 *
 * ---------------------------------------------------------------------------------------------
 * ## 1. Problem
 *
 * A maker can attach free-text trade terms to an offer (`TradeTermsOptionVO.makersTradeTerms`)
 * and keeps a copy on their profile (`UserProfileVO.terms`). Mobile parses both and shows
 * neither, so a taker can commit to a trade without ever seeing the maker's conditions
 * ("I only accept payment from an account in your own name"). Desktop shows them in the offer
 * details overlay (a read-only wrapping text area, 70dp high).
 *
 * ## 2. One component, two surfaces
 *
 * [TradeTermsBlock] is the only trade-terms composable. It is driven by [TradeTermsUiState]:
 *
 * | Surface                 | `displayMode`                       | Shows                                              |
 * |-------------------------|-------------------------------------|----------------------------------------------------|
 * | Take-offer review step  | [TradeTermsDisplayMode.COLLAPSIBLE] | 3 lines + "Show more" when the text overflows      |
 * | Peer profile            | [TradeTermsDisplayMode.FULL]        | The full text, no expander                         |
 *
 * The review step is a decision screen with a sticky bottom bar, so the terms must not push the
 * price, fee and the "Take offer" button out of reach: they collapse. The peer profile is a
 * reading screen that already scrolls, and the terms are the reason a user opens it, so they are
 * shown in full.
 *
 * The data source differs and is deliberately NOT unified:
 * - Review step: the offer's `TradeTermsOptionVO.makersTradeTerms`. This is the text that was
 *   attached to the offer at creation and is what the taker agrees to, even if the maker has
 *   edited their profile terms since.
 * - Peer profile: `UserProfileVO.terms`, the maker's current terms.
 *
 * ## 3. Layout
 *
 * ```
 * Maker's trade terms                        <- label, same style as the other review labels
 * |  I only accept payment from an account   <- quote bar + text, max 3 lines when collapsed
 * |  in your own name. No third party pay...
 * Show more  v                               <- 48dp-high tap row, only when the text overflows
 *
 * By taking this offer you accept the        <- review step only, display only, no checkbox
 * maker's trade terms.
 * ```
 *
 * - The label uses the same grey label style as `InfoBox` so the block reads as one more row of
 *   the review.
 * - The text sits next to a 3dp `mid_grey10` vertical bar (quote styling). The bar marks the text
 *   as the maker's own words rather than app copy. In a peer-to-peer app the UI must not look as
 *   if Bisq itself vouches for or authored those conditions. It is not a card, so it cannot be
 *   confused with the private "Contact details" card on the peer profile, which holds the user's
 *   own notes about the peer.
 * - Text style `baseRegular`, white. The terms are the content the user must read, so they use
 *   full-contrast text, unlike the grey helper lines.
 * - Gap label to text: `ScreenPaddingHalf` (6dp). Text to expander: no extra gap, the expander row
 *   is 48dp high on its own. Expander to acceptance line: `ScreenPaddingHalf`.
 * - The expander is a chevron that rotates 180 degrees, as in the network connection card.
 *
 * ## 4. Placement
 *
 * - Review step: a third section after the fee `InfoBox`, separated from "the deal" by a
 *   `BisqHDivider`, still inside the scrolling content and above the sticky bottom bar. Terms
 *   are conditions on the deal, not a number, so they get their own section. The acceptance line
 *   is the last element of the scroll content, immediately above the bottom bar's "Take offer"
 *   button, so the notice is read last, right before committing.
 * - Peer profile: after the contact-details section (when present) and directly above the peer
 *   offers section. The terms apply to those offers, and the identity header plus the action
 *   buttons keep their current positions, so a long text never pushes the primary action off
 *   screen. A maker's terms therefore also cover the offers list: the same terms are shown again,
 *   in context, on the review step of every offer taken from that list.
 *
 * ## 5. State and behaviour
 *
 * | Case                                   | Behaviour                                                              |
 * |----------------------------------------|------------------------------------------------------------------------|
 * | `tradeTerms == null` in the screen state | Nothing is composed: no label, no gap, no divider, no acceptance line |
 * | Blank text (only whitespace/newlines)  | Treated as no terms: the presenter maps it to `null`                   |
 * | Text fits in 3 lines                   | Text only. No expander, no "Show more"                                 |
 * | Text longer than 3 lines, collapsed    | 3 lines, ellipsis on the last, "Show more" + chevron down              |
 * | Text longer than 3 lines, expanded     | Whole text wraps, "Show less" + chevron up                             |
 * | Very long single word or URL           | Breaks across lines at any character. It never scrolls horizontally   |
 * | Many short lines or blank lines        | Wrapped as typed. Collapsed counts visual lines, so blank lines use up the 3 |
 * | Font scale up to 200% or long locale   | Same rules. The collapsed state is always 3 lines whatever their height |
 * | Right-to-left terms                    | `TextAlign.Start`: aligned and wrapped according to the text direction |
 * | Screen rotated or process recreated    | Expanded flag lives in `UiState`, so it survives configuration change  |
 *
 * Text handling rules (the text is untrusted, it comes from the counterparty):
 * - Render verbatim: trimmed at both ends, never translated, never parsed.
 * - No link detection. A URL is plain text. A tappable link inside the take-offer flow would let a
 *   maker send the taker to an arbitrary site at the most sensitive moment of the flow.
 * - No markdown or HTML interpretation.
 * - Collapsed state uses `TextOverflow.Ellipsis`; expanded state uses no `maxLines`.
 *
 * The expander appears only when the text actually overflows, measured with
 * `TextLayoutResult.hasVisualOverflow` while collapsed. This keeps short terms free of a useless
 * control and adapts to width and font scale. Once the user has expanded, the control stays so the
 * state can be reverted.
 *
 * ## 6. Interaction
 *
 * - Tapping the expander row toggles [TradeTermsUiState.isExpanded] via
 *   [TradeTermsUiAction.OnToggleExpanded]. The expansion is not animated in height (a layout
 *   animation on text that can span many lines is janky on low-end devices); only the chevron
 *   rotates. Respect the app's "use animations" setting: when off, the chevron snaps.
 * - Collapsing does not scroll the screen. If the expanded block was taller than the viewport and
 *   the user collapses from the bottom, the block's top may leave the viewport. This is accepted.
 * - The acceptance line is display-only. There is no checkbox and the "Take offer" button is not
 *   gated on the terms: the terms are disclosed and acknowledged by notice, not by an extra tap.
 *
 * ## 7. Accessibility
 *
 * - Label: `heading()` semantics so TalkBack/VoiceOver heading navigation reaches the terms.
 * - Reading order: label, terms text, expander, acceptance notice, then the bottom bar buttons.
 * - The terms text node always carries the FULL text. Compose exposes the complete string to the
 *   accessibility tree even while visually truncated, so screen-reader users hear everything
 *   without having to expand.
 * - The expander is a `Role.Button` with a `stateDescription` ("Collapsed" / "Expanded") and a
 *   flipping visible label ("Show more" / "Show less"). The chevron icon is decorative
 *   (`contentDescription = null`).
 * - Expander tap target: at least 48dp high and the full width of the block.
 * - Contrast: white text, `mid_grey20` label (the existing label colour), acceptance line
 *   `light_grey20`. All above WCAG AA on `backgroundColor`.
 * - Test tags: `tradeTerms.block`, `tradeTerms.text`, `tradeTerms.toggle`,
 *   `tradeTerms.acceptanceNotice`.
 *
 * ## 8. i18n
 *
 * Reused (already translated):
 * - `bisqEasy.offerDetails.makersTradeTerms` = "Makers trade terms" (review step label)
 * - `user.profileCard.overview.tradeTerms` = "Trade terms" (peer profile label)
 *
 * New, in `shared/domain/src/commonMain/resources/mobile/mobile.properties` only, English only:
 * - `mobile.tradeTerms.showMore` = Show more
 * - `mobile.tradeTerms.showLess` = Show less
 * - `mobile.tradeTerms.state.collapsed` = Collapsed
 * - `mobile.tradeTerms.state.expanded` = Expanded
 * - `mobile.tradeTerms.acceptanceNotice` = By taking this offer you accept the maker's trade terms.
 *
 * Text expansion: German and Russian labels run 30-40% longer. "Show more" is a single row element
 * that can wrap to two lines inside the 48dp row, and the acceptance notice wraps freely. See the
 * long-translation preview.
 *
 * ## 9. Data and presenter contract
 *
 * - `TakeOfferReviewPresenter` exposes `tradeTerms: TradeTermsUiState?`, built from the offer's
 *   `TradeTermsOptionVO` (look for the option in `offerOptions`; absent or blank gives `null`).
 *   It handles [TradeTermsUiAction.OnToggleExpanded] by flipping `isExpanded`.
 * - `PeerProfileUiState` gets `tradeTerms: TradeTermsUiState?` built from `UserProfileVO.terms`
 *   with `displayMode = FULL`. No action handling is needed there.
 * - Both composables only render. The overflow measurement is the one piece of local UI state
 *   (`remember`) because it depends on layout, not on business state.
 *
 * ## 10. Tests to add
 *
 * - Presenter: `TakeOfferReviewPresenter` builds `tradeTerms == null` for an offer without a
 *   `TradeTermsOptionVO`, for blank terms, and non-null with the trimmed text otherwise; toggle
 *   action flips `isExpanded`.
 * - Presenter: `PeerProfilePresenter` maps `UserProfileVO.terms` to `null` when blank and to a
 *   `FULL` state otherwise.
 * - UI (review step): with terms, `tradeTerms.block` and `tradeTerms.acceptanceNotice` exist;
 *   without terms neither exists. With long terms `tradeTerms.toggle` exists and tapping it
 *   dispatches the toggle action; with short terms the toggle does not exist.
 * - UI (peer profile): terms present shows `tradeTerms.text` with the full string and no
 *   `tradeTerms.toggle`; absent shows nothing.
 *
 * TODO: Confirm the maximum length bisq2 enforces for profile terms when they are entered. If it
 * is unbounded, cap the rendered length on the review step (for example 2,000 characters with the
 * remainder truncated and flagged) so a hostile maker cannot make the review step arbitrarily
 * long.
 * TODO: Decide whether the terms text should be selectable. It is useful for copying an IBAN-like
 * detail into a chat, but selection handles inside a scrolling wizard step are fiddly. It is left
 * out until a user need is reported.
 */
internal const val TRADE_TERMS_COLLAPSED_MAX_LINES = 3

/** Which treatment [TradeTermsBlock] renders. */
internal enum class TradeTermsDisplayMode {
    /** Review step: 3 lines plus an expander when the text overflows. */
    COLLAPSIBLE,

    /** Peer profile: always the full text, no expander. */
    FULL,
}

/** Everything [TradeTermsBlock] needs. A screen holds `null` instead of this when there are no terms. */
internal data class TradeTermsUiState(
    val text: String,
    val displayMode: TradeTermsDisplayMode,
    val isExpanded: Boolean = false,
)

internal sealed interface TradeTermsUiAction {
    data object OnToggleExpanded : TradeTermsUiAction
}

/** Resolved strings, so the block itself never touches the i18n layer. */
internal data class TradeTermsLabels(
    val title: String,
    val showMore: String,
    val showLess: String,
    val collapsedState: String,
    val expandedState: String,
)

/**
 * The shared trade-terms block. Stateless apart from the overflow measurement, which is layout
 * dependent. Renders nothing for blank text, so a caller may also pass an unchecked string.
 */
@ExcludeFromCoverage
@Composable
internal fun TradeTermsBlock(
    state: TradeTermsUiState,
    labels: TradeTermsLabels,
    onAction: (TradeTermsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = state.text.trim()
    if (text.isEmpty()) return

    val isCollapsible = state.displayMode == TradeTermsDisplayMode.COLLAPSIBLE
    val isCollapsed = isCollapsible && !state.isExpanded
    var overflows by remember(text) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().testTag("tradeTerms.block"),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqText.BaseLightGrey(
            text = labels.title,
            modifier = Modifier.semantics { heading() },
        )

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                    Modifier
                        .width(BisqUIConstants.ScreenPaddingQuarter)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(BisqTheme.colors.mid_grey10),
            )
            BisqText.StyledText(
                text = text,
                style = BisqTheme.typography.baseRegular,
                textAlign = TextAlign.Start,
                maxLines = if (isCollapsed) TRADE_TERMS_COLLAPSED_MAX_LINES else Int.MAX_VALUE,
                overflow = if (isCollapsed) TextOverflow.Ellipsis else TextOverflow.Clip,
                onTextLayout = { layout -> if (isCollapsed) overflows = layout.hasVisualOverflow },
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = BisqUIConstants.ScreenPadding)
                        .testTag("tradeTerms.text"),
            )
        }

        if (isCollapsible && (overflows || state.isExpanded)) {
            TradeTermsExpander(
                isExpanded = state.isExpanded,
                labels = labels,
                onClick = { onAction(TradeTermsUiAction.OnToggleExpanded) },
            )
        }
    }
}

@Composable
private fun TradeTermsExpander(
    isExpanded: Boolean,
    labels: TradeTermsLabels,
    onClick: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "tradeTermsChevronRotation",
    )
    val stateText = if (isExpanded) labels.expandedState else labels.collapsedState
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { stateDescription = stateText }
                .testTag("tradeTerms.toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqText.SmallMedium(
            text = if (isExpanded) labels.showLess else labels.showMore,
            color = BisqTheme.colors.primary,
        )
        ArrowDownIcon(modifier = Modifier.size(12.dp).rotate(chevronRotation))
    }
}

/** Display-only line under the terms on the review step. Never a checkbox. */
@ExcludeFromCoverage
@Composable
internal fun TradeTermsAcceptanceNotice(
    text: String,
    modifier: Modifier = Modifier,
) {
    BisqText.SmallRegular(
        text = text,
        color = BisqTheme.colors.light_grey20,
        modifier = modifier.fillMaxWidth().testTag("tradeTerms.acceptanceNotice"),
    )
}

// =============================================================================================
// Preview support (primitives only)
// =============================================================================================

internal const val SIMULATED_SHORT_TERMS = "I only accept payment from an account in your own name."

internal const val SIMULATED_LONG_TERMS =
    "I only accept payment from an account in your own name. No third party payments, no " +
        "payments from business accounts and no cash deposits. Please send the payment within 30 " +
        "minutes of the trade starting and write nothing in the payment reference. I answer " +
        "messages between 08:00 and 20:00 UTC. If I do not hear from you within an hour I will " +
        "ask the mediator to step in."

internal const val SIMULATED_URL_TERMS =
    "Payment details and rules: https://example.org/very/long/path/that/never/breaks/anywhere/" +
        "trade-terms-for-bisq-easy-buyers-2026?utm_source=profile&utm_medium=offer&utm_campaign=terms " +
        "AVeryLongSingleWordWithoutAnySpacesThatKeepsGoingAndGoingAndGoingUntilItHitsTheEdge"

internal const val SIMULATED_MULTILINE_TERMS =
    "1. Own-name account only.\n\n\n2. Reference field stays empty.\n\n3. Pay within 30 minutes.\n\n" +
        "4. No chargebacks, no disputes outside Bisq."

internal const val SIMULATED_LONG_TRANSLATION_TERMS =
    "Ich akzeptiere ausschliesslich Zahlungen von einem Konto, das auf Ihren eigenen Namen " +
        "lautet. Zahlungen durch Dritte, Geschaeftskonten und Bareinzahlungen werden " +
        "grundsaetzlich nicht akzeptiert. Bitte ueberweisen Sie innerhalb von dreissig Minuten " +
        "nach Handelsbeginn und lassen Sie das Verwendungszweckfeld vollstaendig leer."

internal fun simulatedTradeTermsLabels(
    title: String = "Makers trade terms",
    showMore: String = "Show more",
    showLess: String = "Show less",
): TradeTermsLabels =
    TradeTermsLabels(
        title = title,
        showMore = showMore,
        showLess = showLess,
        collapsedState = "Collapsed",
        expandedState = "Expanded",
    )

internal const val SIMULATED_ACCEPTANCE_NOTICE = "By taking this offer you accept the maker's trade terms."

/**
 * Restage of the take-offer review step with the new terms section and the new price wording
 * (see the offer-card price design). Uses the real wizard scaffold, so the step bar, the sticky
 * bottom bar and the scroll behaviour are the production ones.
 */
@ExcludeFromCoverage
@Composable
internal fun SimulatedTakeOfferReview(
    tradeTerms: TradeTermsUiState?,
    priceTypeLabel: String,
    modifier: Modifier = Modifier,
    labels: TradeTermsLabels = simulatedTradeTermsLabels(),
    acceptanceNotice: String = SIMULATED_ACCEPTANCE_NOTICE,
    onAction: (TradeTermsUiAction) -> Unit = {},
) {
    MultiScreenWizardScaffold(
        title = "Review",
        stepIndex = 4,
        stepsLength = 4,
        prevOnClick = {},
        nextButtonText = "Take offer",
        nextOnClick = {},
        showUserAvatar = false,
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            BisqGap.V1()
            InfoBox(label = "DIRECTION", value = "Buy Bitcoin from Satoshi")
            InfoRowContainer {
                InfoBoxCurrency(label = "AMOUNT TO PAY", value = "500 USD")
                InfoBoxSats(label = "AMOUNT TO RECEIVE", value = "0.0050")
            }
        }

        BisqHDivider()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            InfoBox(
                label = "PRICE",
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            BisqText.H6Light("45,900")
                            BisqGap.HQuarter()
                            BisqText.BaseLightGrey("USD/BTC")
                        }
                        BisqText.SmallRegular(text = priceTypeLabel, color = BisqTheme.colors.light_grey20)
                        BisqText.SmallLightGrey("Float price. 2.00 % above market price of 45,000 USD/BTC")
                    }
                },
            )
            InfoBox(label = "PAYMENT METHOD", value = "SEPA, Zelle")
            InfoBox(label = "BITCOIN SETTLEMENT", value = "On-chain")
            InfoBox(label = "FEE", value = "0.50 USD", subvalue = "Trade fee (0.1%)")
        }

        if (tradeTerms != null) {
            BisqHDivider()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                TradeTermsBlock(state = tradeTerms, labels = labels, onAction = onAction)
                TradeTermsAcceptanceNotice(text = acceptanceNotice)
            }
        }
    }
}

/** Restage of the peer profile body from the header down to the offers section. */
@ExcludeFromCoverage
@Composable
internal fun SimulatedPeerProfile(
    tradeTerms: TradeTermsUiState?,
    modifier: Modifier = Modifier,
    labels: TradeTermsLabels = simulatedTradeTermsLabels(title = "Trade terms"),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.mid_grey10),
        )
        BisqGap.V1()
        BisqText.H5Regular(text = "Satoshi", color = BisqTheme.colors.white, textAlign = TextAlign.Center)
        BisqGap.VHalf()
        StarRating(rating = 4.5)
        BisqGap.VHalf()
        BisqText.BaseLightGrey(text = "Reputation score: 52,300", textAlign = TextAlign.Center)
        BisqGap.V2()
        BisqButton(text = "Send private message", onClick = {}, fullWidth = true)
        BisqGap.VHalf()
        BisqButton(text = "Ignore user", onClick = {}, type = BisqButtonType.GreyOutline, fullWidth = true)
        BisqGap.VHalf()
        BisqButton(text = "Report user", onClick = {}, fullWidth = true)

        if (tradeTerms != null) {
            BisqGap.V2()
            Column(modifier = Modifier.fillMaxWidth()) {
                TradeTermsBlock(state = tradeTerms, labels = labels, onAction = {})
            }
        }

        BisqGap.V2()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqText.BaseRegular("Offers by Satoshi")
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(BisqTheme.colors.dark_grey50),
            )
        }
    }
}

private fun collapsibleTerms(
    text: String,
    isExpanded: Boolean = false,
) = TradeTermsUiState(text = text, displayMode = TradeTermsDisplayMode.COLLAPSIBLE, isExpanded = isExpanded)

private fun fullTerms(text: String) = TradeTermsUiState(text = text, displayMode = TradeTermsDisplayMode.FULL)

// =============================================================================================
// Previews: review step
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Review 1: no terms (block, divider and notice absent)", heightDp = 900)
@Composable
private fun Review_NoTerms_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(tradeTerms = null, priceTypeLabel = "+2.00 % vs market")
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 2: short terms (no expander)", heightDp = 900)
@Composable
private fun Review_ShortTerms_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_SHORT_TERMS),
            priceTypeLabel = "+2.00 % vs market",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 3: long terms, collapsed", heightDp = 900)
@Composable
private fun Review_LongTermsCollapsed_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_LONG_TERMS),
            priceTypeLabel = "+2.00 % vs market",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 4: long terms, expanded", heightDp = 1100)
@Composable
private fun Review_LongTermsExpanded_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_LONG_TERMS, isExpanded = true),
            priceTypeLabel = "+2.00 % vs market",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 5: URL and very long word, collapsed", heightDp = 900)
@Composable
private fun Review_UrlCollapsed_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_URL_TERMS),
            priceTypeLabel = "Fixed",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 6: URL and very long word, expanded", heightDp = 1100)
@Composable
private fun Review_UrlExpanded_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_URL_TERMS, isExpanded = true),
            priceTypeLabel = "Fixed",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 7: blank lines use up the 3 collapsed lines", heightDp = 900)
@Composable
private fun Review_MultilineCollapsed_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_MULTILINE_TERMS),
            priceTypeLabel = "-1.50 % vs market",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 8: long translation, collapsed", heightDp = 900)
@Composable
private fun Review_LongTranslationCollapsed_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_LONG_TRANSLATION_TERMS),
            priceTypeLabel = "+2.00 % vs market",
            labels =
                simulatedTradeTermsLabels(
                    title = "Handelsbedingungen des Anbieters",
                    showMore = "Mehr anzeigen",
                    showLess = "Weniger anzeigen",
                ),
            acceptanceNotice =
                "Mit der Annahme dieses Angebots akzeptieren Sie die Handelsbedingungen des " +
                    "Anbieters.",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 9: large font (fontScale 2.0), collapsed", heightDp = 1200, fontScale = 2f)
@Composable
private fun Review_LargeFontCollapsed_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_LONG_TERMS),
            priceTypeLabel = "+2.00 % vs market",
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Review 10: small screen (320dp wide)", widthDp = 320, heightDp = 900)
@Composable
private fun Review_SmallScreen_Preview() {
    BisqTheme.Preview {
        SimulatedTakeOfferReview(
            tradeTerms = collapsibleTerms(SIMULATED_LONG_TERMS),
            priceTypeLabel = "+2.00 % vs market",
        )
    }
}

// =============================================================================================
// Previews: block in isolation
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Block: collapsed vs expanded side by side", heightDp = 520)
@Composable
private fun Block_CollapsedAndExpanded_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            TradeTermsBlock(
                state = collapsibleTerms(SIMULATED_LONG_TERMS),
                labels = simulatedTradeTermsLabels(),
                onAction = {},
            )
            TradeTermsBlock(
                state = collapsibleTerms(SIMULATED_LONG_TERMS, isExpanded = true),
                labels = simulatedTradeTermsLabels(),
                onAction = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Block: whitespace-only text renders nothing", heightDp = 120)
@Composable
private fun Block_BlankText_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding2X),
        ) {
            BisqText.SmallLightGrey("Nothing is composed between these two lines:")
            TradeTermsBlock(
                state = collapsibleTerms("   \n  "),
                labels = simulatedTradeTermsLabels(),
                onAction = {},
            )
            BisqText.SmallLightGrey("(end)")
        }
    }
}

// =============================================================================================
// Previews: peer profile
// =============================================================================================

@ExcludeFromCoverage
@Preview(name = "Peer profile 1: without terms (section absent)", heightDp = 800)
@Composable
private fun PeerProfile_NoTerms_Preview() {
    BisqTheme.Preview {
        SimulatedPeerProfile(tradeTerms = null)
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer profile 2: short terms", heightDp = 800)
@Composable
private fun PeerProfile_ShortTerms_Preview() {
    BisqTheme.Preview {
        SimulatedPeerProfile(tradeTerms = fullTerms(SIMULATED_SHORT_TERMS))
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer profile 3: long terms shown in full", heightDp = 900)
@Composable
private fun PeerProfile_LongTerms_Preview() {
    BisqTheme.Preview {
        SimulatedPeerProfile(tradeTerms = fullTerms(SIMULATED_LONG_TERMS))
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer profile 4: URL and very long word", heightDp = 900)
@Composable
private fun PeerProfile_UrlTerms_Preview() {
    BisqTheme.Preview {
        SimulatedPeerProfile(tradeTerms = fullTerms(SIMULATED_URL_TERMS))
    }
}

@ExcludeFromCoverage
@Preview(name = "Peer profile 5: large font (fontScale 2.0)", heightDp = 1500, fontScale = 2f)
@Composable
private fun PeerProfile_LargeFont_Preview() {
    BisqTheme.Preview {
        SimulatedPeerProfile(tradeTerms = fullTerms(SIMULATED_LONG_TERMS))
    }
}
