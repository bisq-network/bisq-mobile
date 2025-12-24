package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun InterruptedTradePane() {
    val presenter: InterruptedTradePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val interruptionInfoVisible by presenter.interruptionInfoVisible.collectAsState()
    val interruptedTradeInfo by presenter.interruptedTradeInfo.collectAsState()
    val errorMessageVisible by presenter.errorMessageVisible.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()
    val reportToMediatorButtonVisible by presenter.reportToMediatorButtonVisible.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = BisqTheme.colors.dark_grey40),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            if (interruptionInfoVisible) {
                // TODO: Add orange warn icon
                BisqText.BaseMedium(
                    text = interruptedTradeInfo,
                    color = BisqTheme.colors.warning,
                )
            }
            if (errorMessageVisible) {
                // TODO: Add red warn icon
                BisqText.BaseMedium(
                    text = presenter.errorMessage,
                    color = BisqTheme.colors.danger,
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isInMediation && reportToMediatorButtonVisible) {
                BisqButton(
                    modifier = Modifier.fillMaxHeight(),
                    text = "bisqEasy.openTrades.reportToMediator".i18n(),
                    onClick = { presenter.onReportToMediator() },
                    type = BisqButtonType.Outline,
                    color = BisqTheme.colors.primary,
                    borderColor = BisqTheme.colors.primary,
                )
                BisqGap.H1()
            }
            BisqButton(
                modifier = Modifier.fillMaxHeight(),
                text = "action.close".i18n(),
                onClick = { presenter.onCloseTrade() },
            )
        }
    }
}
