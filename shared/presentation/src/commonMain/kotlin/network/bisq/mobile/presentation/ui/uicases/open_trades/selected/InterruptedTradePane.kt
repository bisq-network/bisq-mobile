package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun InterruptedTradePane() {
    val presenter: InterruptedTradePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val interruptionInfoVisible by presenter.interruptionInfoVisible.collectAsState()
    val interruptedTradeInfo by presenter.interruptedTradeInfo.collectAsState()
    val errorMessageVisible by presenter.errorMessageVisible.collectAsState()
    val errorMessage by remember { mutableStateOf(presenter.errorMessage) }
    val isInMediation by presenter.isInMediation.collectAsState()
    val reportToMediatorButtonVisible by presenter.reportToMediatorButtonVisible.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(12.dp))
            .background(color = BisqTheme.colors.dark_grey40)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            if (interruptionInfoVisible) {
                // TODO: Add orange warn icon
                BisqText.baseMedium(
                    text = interruptedTradeInfo,
                    color = BisqTheme.colors.warning
                )
            }
            if (errorMessageVisible) {
                // TODO: Add red warn icon
                BisqText.baseMedium(
                    text = errorMessage,
                    color = BisqTheme.colors.danger
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isInMediation && reportToMediatorButtonVisible) {
                BisqButton(
                    text = "bisqEasy.openTrades.reportToMediator".i18n(),
                    onClick = { presenter.onReportToMediator() },
                    type = BisqButtonType.Outline,
                    color = BisqTheme.colors.primary,
                    borderColor = BisqTheme.colors.primary,
                )
                BisqGap.H1()
            }
            BisqButton(
                text = "Close",
                onClick = { presenter.onCloseTrade() },
            )
        }
    }
}

