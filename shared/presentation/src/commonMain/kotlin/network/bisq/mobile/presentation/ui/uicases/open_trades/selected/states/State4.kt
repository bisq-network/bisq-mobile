package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_completed
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

@Composable
fun State4(
    presenter: State4Presenter,
) {
    RememberPresenterLifecycle(presenter)

    val tradeItemModel = presenter.selectedTrade.value
    val quoteAmount = tradeItemModel?.quoteAmountWithCode ?: ""
    val baseAmount = tradeItemModel?.formattedBaseAmount ?: ""

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
            BisqText.h5Light("bisqEasy.tradeCompleted.title".i18n())
        }

        Column {
            BisqGap.V2()

            BtcSatsText(
                baseAmount,
                label = presenter.getMyDirectionString(),
                style = BtcSatsStyle.TextField
            )
            BisqGap.VHalf()
            BisqTextField(
                label = presenter.getMyOutcomeString(),
                value = quoteAmount,
                disabled = true
            )

            BisqGap.V2()
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
//                BisqButton(
//                    text = "bisqEasy.tradeState.info.phase4.exportTrade".i18n(), // Export trade data
//                    type = BisqButtonType.Grey,
//                    onClick = { presenter.onExportTradeDate() },
//                )
                BisqButton(
                    text = "bisqEasy.tradeState.info.phase4.leaveChannel".i18n(), // Close trade
                    onClick = { presenter.onCloseTrade() },
                )
            }

        }
    }

}
