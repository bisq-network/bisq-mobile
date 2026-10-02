@file:Suppress("ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.security.SecureScreenEffect
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle

const val BANNED_ACCOUNT_BANNER_TAG = "banned_account_banner"

@Composable
fun BuyerState2a(
    presenter: BuyerState2aPresenter,
) {
    RememberPresenterLifecycle(presenter)
    SecureScreenEffect()

    val uiState by presenter.uiState.collectAsState()
    BuyerState2aContent(uiState = uiState, onAction = presenter::onAction)
}

@Composable
private fun BuyerState2aContent(
    uiState: BuyerState2aUiState,
    onAction: (BuyerState2aUiAction) -> Unit,
) {
    if (!uiState.isTradeLoaded) return

    val quoteAmount = uiState.quoteAmountWithCode
    val paymentAccountData = uiState.paymentAccountData ?: "data.na".i18n()

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        // Send {0} to the seller''s payment account
        BisqText.H5Light("bisqEasy.tradeState.info.buyer.phase2a.headline".i18n(quoteAmount))

        BisqGap.VHalf()
        BisqTextFieldV0(
            // Amount to transfer
            label = "bisqEasy.tradeState.info.buyer.phase2a.quoteAmount".i18n(),
            value = quoteAmount,
            enabled = false,
            trailingIcon = { CopyIconButton(value = quoteAmount) },
        )

        BisqGap.VHalf()
        BisqTextFieldV0(
            // Payment account of seller
            label = "bisqEasy.tradeState.info.buyer.phase2a.sellersAccount".i18n(),
            // In Bisq Easy we show the Reason for payment with the trade ID as extra field, but on mobile we don't want to
            // use up too much space for that and show it as helper text instead. The fraud warning takes that slot when banned.
            bottomMessage =
                if (uiState.isAccountDataBanned) {
                    // The seller's account data has been used in fraudulent activities
                    "bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError".i18n()
                } else {
                    // Use the trade ID {0} for the 'Reason for payment' field
                    "mobile.tradeState.info.buyer.phase2a.reasonForPaymentInfo".i18n(uiState.tradeShortId)
                },
            isError = uiState.isAccountDataBanned,
            value = paymentAccountData,
            enabled = false,
            trailingIcon = { CopyIconButton(value = paymentAccountData) },
            maxLines = Int.MAX_VALUE,
            minLines = 2,
        )

        if (uiState.isAccountDataBanned) {
            BisqGap.V1()
            BannedAccountBanner()
        }

        BisqGap.V1()
        BisqButton(
            // Confirm payment of {0}
            text = "bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent".i18n(quoteAmount),
            onClick = { onAction(BuyerState2aUiAction.OnConfirmFiatSent) },
            disabled = !uiState.isConfirmFiatSentEnabled,
        )
    }

    if (uiState.isBannedWarningVisible) {
        BannedAccountWarningDialog(onAcknowledge = { onAction(BuyerState2aUiAction.OnAcknowledgeBannedWarning) })
    }
}

/** Danger banner right above the disabled confirm button, so the reason is read together with it. */
@Composable
private fun BannedAccountBanner() {
    val shape = RoundedCornerShape(BisqUIConstants.BorderRadius)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = BisqTheme.colors.danger.copy(alpha = 0.15f), shape = shape)
                .border(width = 3.dp, color = BisqTheme.colors.danger, shape = shape)
                .padding(BisqUIConstants.ScreenPadding)
                .testTag(BANNED_ACCOUNT_BANNER_TAG),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.Top,
    ) {
        ExclamationRedIcon()
        Column {
            BisqText.SmallMedium(
                // The seller's account data has been used in fraudulent activities
                text = "bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError".i18n(),
                color = BisqTheme.colors.danger,
            )
            BisqGap.VQuarter()
            BisqText.SmallLight(
                text = "mobile.tradeState.info.buyer.phase2a.accountDataBanned.banner.action".i18n(),
                color = BisqTheme.colors.light_grey10,
            )
        }
    }
}

@Composable
private fun BannedAccountWarningDialog(onAcknowledge: () -> Unit) {
    ConfirmationDialog(
        headline = "popup.headline.warning".i18n(),
        headlineColor = BisqTheme.colors.danger,
        headlineLeftIcon = { ExclamationRedIcon() },
        message = "bisqEasy.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning".i18n(),
        confirmButtonText = "action.iUnderstand".i18n(),
        dismissButtonText = EMPTY_STRING,
        dismissOnClickOutside = false,
        onConfirm = onAcknowledge,
        onDismiss = { onAcknowledge() },
    )
}

private val previewUiState =
    BuyerState2aUiState(
        isTradeLoaded = true,
        quoteAmountWithCode = "250.00 EUR",
        paymentAccountData = "DE89 3704 0044 0532 0130 00\nAccount owner: Max Mustermann",
        tradeShortId = "8f3ac210",
        isConfirmFiatSentEnabled = true,
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState2aContent_NotBannedPreview() {
    BisqTheme.Preview {
        BuyerState2aContent(uiState = previewUiState, onAction = {})
    }
}

@ExcludeFromCoverage
@Preview(heightDp = 900)
@Composable
private fun BuyerState2aContent_BannedWarningPreview() {
    BisqTheme.Preview {
        BuyerState2aContent(
            uiState =
                previewUiState.copy(
                    isConfirmFiatSentEnabled = false,
                    isAccountDataBanned = true,
                    isBannedWarningVisible = true,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState2aContent_BannedAcknowledgedPreview() {
    BisqTheme.Preview {
        BuyerState2aContent(
            uiState = previewUiState.copy(isConfirmFiatSentEnabled = false, isAccountDataBanned = true),
            onAction = {},
        )
    }
}
