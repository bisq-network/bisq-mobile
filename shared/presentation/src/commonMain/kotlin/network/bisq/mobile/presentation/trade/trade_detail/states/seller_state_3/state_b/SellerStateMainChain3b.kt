@file:Suppress("ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_3.state_b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_bitcoin_confirmation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TxConfirmationState.CONFIRMED
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TxConfirmationState.FAILED
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TxConfirmationState.IDLE
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TxConfirmationState.IN_MEMPOOL
import network.bisq.mobile.presentation.trade.trade_detail.states.common.TxConfirmationState.REQUEST_STARTED

@Composable
fun SellerStateMainChain3b(
    presenter: SellerStateMainChain3bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val selectedTrade by presenter.selectedTrade.collectAsState()
    val confirmationState by presenter.txConfirmationState.collectAsState()
    val btcBalance by presenter.balanceFromTx.collectAsState()
    val buttonText by presenter.buttonText.collectAsState()
    val explorerRequestError by presenter.errorMessage.collectAsState()
    val blockExplorer by presenter.blockExplorer.collectAsState()
    val txId = selectedTrade?.bisqEasyTradeModel?.paymentProof?.value ?: "data.na".i18n()
    val waitingText =
        "bisqEasy.tradeState.info.seller.phase3b.balance.prompt".i18n(txId) // Waiting for blockchain data...
    val balanceLabel = "bisqEasy.tradeState.info.seller.phase3b.balance".i18n() // Bitcoin payment
    val skip by presenter.skip.collectAsState()
    val amountNotMatchingDialogText by presenter.amountNotMatchingDialogText.collectAsState()

    Column {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_bitcoin_confirmation,
                isLoading = true,
            )
            // Waiting for blockchain confirmation
            BisqText.H5Light("bisqEasy.tradeState.info.seller.phase3b.headline.MAIN_CHAIN".i18n())
        }

        Column {
            BisqGap.V1()
            BisqText.BaseLightGrey(
                // The Bitcoin payment require at least 1 blockchain confirmation to be considered complete.
                "bisqEasy.tradeState.info.seller.phase3b.info.MAIN_CHAIN".i18n(),
            )

            BisqGap.V1()
            BisqTextFieldV0(
                // Transaction ID
                label = "bisqEasy.tradeState.info.phase3b.txId".i18n(),
                value = txId,
                enabled = false,
                trailingIcon = { CopyIconButton(value = txId) },
            )

            BisqGap.VQuarter()

            when (confirmationState) {
                IDLE,
                REQUEST_STARTED,
                -> {
                    BisqTextFieldV0(
                        label = balanceLabel,
                        placeholder = waitingText,
                        bottomMessage = "bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup".i18n(blockExplorer), // Looking up transaction at block explorer ''{0}''
                        enabled = false,
                        modifier = Modifier.alpha(0.5f),
                    )
                }

                IN_MEMPOOL -> {
                    // todo
                    BisqTextFieldV0(
                        label = balanceLabel,
                        value = btcBalance,
                        bottomMessage = "bisqEasy.tradeState.info.phase3b.balance.help.notConfirmed".i18n(), // Transaction seen in mempool but not confirmed yet
                        color = BisqTheme.colors.warning,
                        enabled = false,
                    )
                }

                CONFIRMED -> {
                    // todo
                    BisqTextFieldV0(
                        label = balanceLabel,
                        value = btcBalance,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        bottomMessage = "bisqEasy.tradeState.info.phase3b.balance.help.confirmed".i18n(), // Transaction is confirmed
                        enabled = false,
                    )
                }

                FAILED -> {
                    // todo
                    BisqTextFieldV0(
                        label = balanceLabel,
                        placeholder = waitingText,
                        enabled = false,
                        isError = explorerRequestError?.isNotEmpty() == true,
                        bottomMessage = explorerRequestError,
                    )
                }
            }

            BisqGap.V1()
            BisqButton(
                text = buttonText,
                type = if (skip) BisqButtonType.Grey else BisqButtonType.Default,
                textComponent = {
                    val contentColor = LocalContentColor.current
                    AutoResizeText(
                        text = buttonText,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                    )
                },
                onClick = { presenter.onCtaClick() },
            )
        }
        amountNotMatchingDialogText?.let { errorDialogText ->
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = errorDialogText,
                confirmButtonText = "bisqEasy.tradeState.info.phase3b.button.next.amountNotMatching.resolved".i18n(),
                dismissButtonText = "action.close".i18n(),
                verticalButtonPlacement = true,
                onConfirm = { presenter.onCompleteTrade() },
                onDismiss = { presenter.onAmountNotMatchingDialogDismiss() },
            )
        }
    }
}
