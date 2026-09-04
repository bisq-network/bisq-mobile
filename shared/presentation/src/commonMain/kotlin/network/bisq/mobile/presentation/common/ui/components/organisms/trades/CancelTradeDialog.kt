package network.bisq.mobile.presentation.common.ui.components.organisms.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade.InterruptReason
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqChip
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqChipSize
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun CancelTradeDialog(
    onCancelConfirm: (InterruptReason) -> Unit,
    onDismiss: () -> Unit,
    isRejection: Boolean,
    isBuyer: Boolean = true,
    // Chips feed the opt-in analytics, so they only show for opted-in users — asking "why?"
    // when the answer goes nowhere would be dishonest. Default false: never show unless proven.
    showReasonChips: Boolean = false,
) {
    val part2: String = "bisqEasy.openTrades.cancelTrade.warning.part2".i18n()
    val warningText1 =
        if (isRejection) {
            "bisqEasy.openTrades.rejectTrade.warning".i18n()
        } else {
            if (isBuyer) {
                "bisqEasy.openTrades.cancelTrade.warning.buyer".i18n(part2)
            } else {
                "bisqEasy.openTrades.cancelTrade.warning.seller".i18n(part2)
            }
        }

    // Optional, single-tap, skippable — feeds the opt-in trade-funnel analytics only (#1711).
    var selectedReason by remember { mutableStateOf<InterruptReason?>(null) }

    WarningConfirmationDialog(
        message = warningText1,
        horizontalAlignment = Alignment.Start,
        marginTop =
            if (isRejection) {
                BisqUIConstants.ScreenPadding8X
            } else {
                BisqUIConstants.ScreenPaddingHalf
            },
        onDismiss = onDismiss,
        onConfirm = { onCancelConfirm(selectedReason ?: InterruptReason.UNSPECIFIED) },
        extraContent =
            if (showReasonChips) {
                {
                    InterruptReasonChips(
                        selectedReason = selectedReason,
                        onReasonClick = { reason ->
                            selectedReason = if (selectedReason == reason) null else reason
                        },
                    )
                }
            } else {
                null
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterruptReasonChips(
    selectedReason: InterruptReason?,
    onReasonClick: (InterruptReason) -> Unit,
) {
    Column {
        // Divider marks the survey as a separate, optional section — not part of the warning copy.
        BisqHDivider(verticalPadding = 0.dp)
        BisqGap.VHalf()
        BisqText.SmallLight(
            "mobile.tradeInterrupt.reasonPrompt".i18n(),
            color = BisqTheme.colors.mid_grey20,
        )
        BisqGap.VHalf()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            InterruptReason.entries
                .filterNot { it == InterruptReason.UNSPECIFIED }
                .forEach { reason ->
                    BisqChip(
                        label = reason.label(),
                        selected = reason == selectedReason,
                        showRemove = false,
                        size = BisqChipSize.Compact,
                        onClick = { onReasonClick(reason) },
                    )
                }
        }
    }
}

private fun InterruptReason.label(): String =
    when (this) {
        InterruptReason.PEER_UNRESPONSIVE -> "mobile.tradeInterrupt.reason.peerUnresponsive".i18n()
        InterruptReason.PRICE_MOVED -> "mobile.tradeInterrupt.reason.priceMoved".i18n()
        InterruptReason.PAYMENT_METHOD_ISSUE -> "mobile.tradeInterrupt.reason.paymentMethodIssue".i18n()
        InterruptReason.NO_PROGRESS -> "mobile.tradeInterrupt.reason.noProgress".i18n()
        InterruptReason.TOO_COMPLEX -> "mobile.tradeInterrupt.reason.tooComplex".i18n()
        InterruptReason.CHANGED_MIND -> "mobile.tradeInterrupt.reason.changedMind".i18n()
        InterruptReason.OTHER -> "mobile.tradeInterrupt.reason.other".i18n()
        InterruptReason.UNSPECIFIED -> ""
    }

@Preview
@Composable
private fun CancelTradeDialog_BuyerPreview() {
    BisqTheme.Preview {
        CancelTradeDialog(
            onCancelConfirm = {},
            onDismiss = {},
            isRejection = false,
            isBuyer = true,
        )
    }
}

@Preview
@Composable
private fun CancelTradeDialog_BuyerWithReasonChipsPreview() {
    BisqTheme.Preview {
        CancelTradeDialog(
            onCancelConfirm = {},
            onDismiss = {},
            isRejection = false,
            isBuyer = true,
            showReasonChips = true,
        )
    }
}

@Preview
@Composable
private fun CancelTradeDialog_SellerWithReasonChipsPreview() {
    BisqTheme.Preview {
        CancelTradeDialog(
            onCancelConfirm = {},
            onDismiss = {},
            isRejection = false,
            isBuyer = false,
            showReasonChips = true,
        )
    }
}

@Preview
@Composable
private fun CancelTradeDialog_RejectionWithReasonChipsPreview() {
    BisqTheme.Preview {
        CancelTradeDialog(
            onCancelConfirm = {},
            onDismiss = {},
            isRejection = true,
            showReasonChips = true,
        )
    }
}
