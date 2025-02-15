package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_completed
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.organisms.GenericErrorPanel
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun State4(
    presenter: State4Presenter,
) {
    RememberPresenterLifecycle(presenter)

    val tradeItemModel = presenter.selectedTrade.value
    val quoteAmount = tradeItemModel?.quoteAmountWithCode ?: ""
    val baseAmount = tradeItemModel?.baseAmountWithCode ?: ""
    val showCloseTradeDialog = presenter.showCloseTradeDialog.collectAsState().value

    Column {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_completed,
                isLoading = true
            )
            // Trade was successfully completed
            BisqText.h5Light(text = "bisqEasy.tradeCompleted.title".i18n())
        }

        Column {
            BisqGap.V2()
            BisqTextField(
                label = "bisqEasy.tradeCompleted.header.myDirection.seller".i18n(), // I sold
                value = baseAmount,
                disabled = true
            )
            BisqGap.VHalf()
            BisqTextField(
                label = "bisqEasy.tradeCompleted.header.myOutcome.seller".i18n(), // I paid
                value = quoteAmount,
                disabled = true
            )

            BisqGap.V2()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                BisqButton(
                    text = "bisqEasy.tradeState.info.phase4.exportTrade".i18n(), // Export trade data
                    onClick = { presenter.onExportTradeDate() },
                    padding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 6.dp
                    ),
                    color = BisqTheme.colors.light1,
                    backgroundColor = BisqTheme.colors.dark5, //todo add BisqButtonType
                )
                BisqButton(
                    text = "bisqEasy.tradeState.info.phase4.leaveChannel".i18n(), // Close trade
                    onClick = { presenter.onCloseTrade() },
                    padding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 6.dp
                    )
                )
            }

            if (showCloseTradeDialog) {
                CloseTradeDialog(
                    onDismissCloseTrade = {
                        presenter.onDismissCloseTrade()
                    },
                    onConfirmCloseTrade = {
                        presenter.onConfirmCloseTrade()
                    }
                )
            }
        }
    }
}
