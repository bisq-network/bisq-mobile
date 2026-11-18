package network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.context.LocalAnimationsEnabled
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

const val TRADE_TERMS_BLOCK_TAG = "tradeTerms.block"
const val TRADE_TERMS_TEXT_TAG = "tradeTerms.text"
const val TRADE_TERMS_TOGGLE_TAG = "tradeTerms.toggle"
const val TRADE_TERMS_ACCEPTANCE_NOTICE_TAG = "tradeTerms.acceptanceNotice"
const val TRADE_TERMS_TRUNCATED_NOTICE_TAG = "tradeTerms.truncatedNotice"

private const val COLLAPSED_MAX_LINES = 3

/**
 * A maker's free-text trade terms, shared by the take-offer review step
 * ([TradeTermsDisplayMode.COLLAPSIBLE]) and the peer profile ([TradeTermsDisplayMode.FULL]).
 *
 * The text comes from the counterparty and is rendered as plain text: no link detection (a
 * tappable link inside the take-offer flow would let a maker send the taker anywhere at the most
 * sensitive moment), no markdown. The quote bar marks it as the maker's own words rather than app
 * copy.
 *
 * The expander appears only when the collapsed text actually overflows, so it adapts to width and
 * font scale; once expanded it stays so the user can collapse again. The text node always carries
 * the full string, so screen readers hear everything without expanding.
 */
@Composable
fun TradeTermsBlock(
    state: TradeTermsUiState,
    title: String,
    modifier: Modifier = Modifier,
    onToggleExpand: () -> Unit = {},
) {
    val isCollapsible = state.displayMode == TradeTermsDisplayMode.COLLAPSIBLE
    val isCollapsed = isCollapsible && !state.isExpanded
    // Layout-dependent, so it lives here rather than in the presenter's state.
    var overflows by remember(state.text) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().testTag(TRADE_TERMS_BLOCK_TAG),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqText.BaseLightGrey(
            text = title,
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
                text = if (state.isTruncated) "${state.text}…" else state.text,
                style = BisqTheme.typography.baseRegular,
                textAlign = TextAlign.Start,
                maxLines = if (isCollapsed) COLLAPSED_MAX_LINES else Int.MAX_VALUE,
                overflow = if (isCollapsed) TextOverflow.Ellipsis else TextOverflow.Clip,
                onTextLayout = { layout -> if (isCollapsed) overflows = layout.hasVisualOverflow },
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = BisqUIConstants.ScreenPadding)
                        .testTag(TRADE_TERMS_TEXT_TAG),
            )
        }

        if (state.isTruncated) {
            BisqText.SmallLightGrey(
                text = "mobile.tradeTerms.truncatedNotice".i18n(),
                modifier = Modifier.testTag(TRADE_TERMS_TRUNCATED_NOTICE_TAG),
            )
        }

        if (isCollapsible && (overflows || state.isExpanded)) {
            TradeTermsExpander(isExpanded = state.isExpanded, onClick = onToggleExpand)
        }
    }
}

@Composable
private fun TradeTermsExpander(
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    // Only the chevron animates: a height animation on text that can span many lines is janky on
    // low-end devices.
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (LocalAnimationsEnabled.current) spring() else snap(),
        label = "tradeTermsChevronRotation",
    )
    val stateText =
        if (isExpanded) "mobile.tradeTerms.state.expanded".i18n() else "mobile.tradeTerms.state.collapsed".i18n()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { stateDescription = stateText }
                .testTag(TRADE_TERMS_TOGGLE_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqText.SmallMedium(
            text = if (isExpanded) "mobile.tradeTerms.showLess".i18n() else "mobile.tradeTerms.showMore".i18n(),
            color = BisqTheme.colors.primary,
        )
        ArrowDownIcon(modifier = Modifier.size(12.dp).rotate(chevronRotation))
    }
}

/** Display-only line under the terms on the review step. Never a checkbox: the terms are acknowledged by notice. */
@Composable
fun TradeTermsAcceptanceNotice(modifier: Modifier = Modifier) {
    BisqText.SmallRegular(
        text = "mobile.tradeTerms.acceptanceNotice".i18n(),
        color = BisqTheme.colors.light_grey20,
        modifier = modifier.fillMaxWidth().testTag(TRADE_TERMS_ACCEPTANCE_NOTICE_TAG),
    )
}

private const val PREVIEW_LONG_TERMS =
    "I only accept payment from an account in your own name. No third party payments, no " +
        "payments from business accounts and no cash deposits. Please send the payment within 30 " +
        "minutes of the trade starting and write nothing in the payment reference."

@ExcludeFromCoverage
@Composable
private fun TradeTermsBlockPreview(state: TradeTermsUiState) {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
            TradeTermsBlock(state = state, title = "Makers trade terms")
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeTermsBlock_Collapsed_Preview() = TradeTermsBlockPreview(TradeTermsUiState(PREVIEW_LONG_TERMS, TradeTermsDisplayMode.COLLAPSIBLE))

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeTermsBlock_Expanded_Preview() = TradeTermsBlockPreview(TradeTermsUiState(PREVIEW_LONG_TERMS, TradeTermsDisplayMode.COLLAPSIBLE, isExpanded = true))

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeTermsBlock_Full_Preview() = TradeTermsBlockPreview(TradeTermsUiState(PREVIEW_LONG_TERMS, TradeTermsDisplayMode.FULL))
