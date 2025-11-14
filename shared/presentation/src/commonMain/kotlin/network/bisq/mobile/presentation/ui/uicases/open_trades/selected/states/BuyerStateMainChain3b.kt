package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_bitcoin_confirmation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.PaymentProofField
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.PaymentProofType
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.CONFIRMED
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.FAILED
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.IDLE
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.IN_MEMPOOL
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TxConfirmationState.REQUEST_STARTED

@Composable
fun BuyerStateMainChain3b(
    presenter: BuyerStateMainChain3bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val selectedTrade by presenter.selectedTrade.collectAsState()

    val tradeItemModel = selectedTrade ?: return
    val confirmationState by presenter.txConfirmationState.collectAsState()
    val btcBalance by presenter.balanceFromTx.collectAsState()
    val buttonText by presenter.buttonText.collectAsState()
    val explorerRequestError by presenter.errorMessage.collectAsState()
    val blockExplorer by presenter.blockExplorer.collectAsState()
    val txId = tradeItemModel.bisqEasyTradeModel.paymentProof.value ?: "data.na".i18n()
    val skip by presenter.skip.collectAsState()
    val amountNotMatchingDialogText by presenter.amountNotMatchingDialogText.collectAsState()

    val role = if (tradeItemModel.bisqEasyTradeModel.isBuyer) "buyer" else "seller"
    val waitingText =
        "bisqEasy.tradeState.info.$role.phase3b.balance.prompt".i18n(txId)  // Waiting for blockchain data...
    val balanceLabel = "bisqEasy.tradeState.info.$role.phase3b.balance".i18n() // Bitcoin payment
    Column {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_bitcoin_confirmation,
                isLoading = true
            )
            // Waiting for blockchain confirmation
            BisqText.h5Light("bisqEasy.tradeState.info.$role.phase3b.headline.MAIN_CHAIN".i18n())
        }

        Column {
            BisqGap.V1()
            BisqText.baseLightGrey(
                // The Bitcoin payment require at least 1 blockchain confirmation to be considered complete.
                "bisqEasy.tradeState.info.$role.phase3b.info.MAIN_CHAIN".i18n(),
            )

            BisqGap.V1()

            PaymentProofField(
                // Transaction ID
                label = "bisqEasy.tradeState.info.phase3b.txId".i18n(),
                value = txId,
                type = PaymentProofType.BitcoinTx,
                disabled = true,
            )

            BisqGap.VQuarter()

            when (confirmationState) {
                IDLE,
                REQUEST_STARTED -> {
                    BisqTextField(
                        label = balanceLabel,
                        placeholder = waitingText,
                        helperText = "bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup".i18n(blockExplorer), // Looking up transaction at block explorer ''{0}''
                        disabled = true,
                        modifier = Modifier.alpha(0.5f),
                        validation = {
                            if (explorerRequestError?.isNotEmpty() == true) {
                                return@BisqTextField explorerRequestError
                            }
                            return@BisqTextField null
                        }
                    )
                }

                IN_MEMPOOL -> {
                    //todo
                    BisqTextField(
                        label = balanceLabel,
                        value = btcBalance,
                        helperText = "bisqEasy.tradeState.info.phase3b.balance.help.notConfirmed".i18n(), // Transaction seen in mempool but not confirmed yet
                        color = BisqTheme.colors.warning,
                        disabled = true,
                        validation = {
                            if (explorerRequestError?.isNotEmpty() == true) {
                                return@BisqTextField explorerRequestError
                            }
                            return@BisqTextField null
                        }
                    )
                }

                CONFIRMED -> {
                    //todo
                    BisqTextField(
                        label = balanceLabel,
                        value = btcBalance,
                        helperText = "bisqEasy.tradeState.info.phase3b.balance.help.confirmed".i18n(), // Transaction is confirmed
                        //color = BisqTheme.colors.primary,
                        disabled = true,
                        validation = {
                            if (explorerRequestError?.isNotEmpty() == true) {
                                return@BisqTextField explorerRequestError
                            }
                            return@BisqTextField null
                        }
                    )
                }

                FAILED -> {
                    //todo
                    BisqTextField(
                        label = balanceLabel,
                        value = waitingText,
                        disabled = true,
                        validation = {
                            if (explorerRequestError?.isNotEmpty() == true) {
                                return@BisqTextField explorerRequestError
                            }
                            return@BisqTextField null
                        }
                    )
                }
            }

            BisqGap.V1()
            BisqButton(
                text = buttonText,
                textComponent = {
                    val contentColor = LocalContentColor.current
                    AutoResizeText(
                       text = buttonText,
                       color = contentColor,
                       textAlign = TextAlign.Center
                    )
                },
                type = if (skip) BisqButtonType.Grey else BisqButtonType.Default,
                onClick = { presenter.onCtaClick() },
            )
        }
        amountNotMatchingDialogText?.let { dialogText ->
            ConfirmationDialog(
                headline = "mobile.error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = dialogText,
                confirmButtonText = "bisqEasy.tradeState.info.phase3b.button.next.amountNotMatching.resolved".i18n(),
                dismissButtonText = "action.close".i18n(),
                verticalButtonPlacement = true,
                onConfirm = { presenter.onCompleteTrade() },
                onDismiss = { presenter.onAmountNotMatchingDialogDismiss() }
            )
        }
    }
}
