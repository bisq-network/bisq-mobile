package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Shown when a trade has been sitting in the INIT state past
 * [network.bisq.mobile.domain.utils.TradeOutOfSyncDetector.OUT_OF_SYNC_THRESHOLD_MS]: the local
 * trade FSM is desynced from the peer and the step tracker below stays frozen with no actionable
 * content, while the peer's side is typically healthy. Renders nothing while the trade is in sync.
 */
@Composable
fun TradeOutOfSyncPane(
    presenter: OpenTradePresenter,
    headerPresenter: TradeDetailsHeaderPresenter,
) {
    val isTradeOutOfSync by presenter.isTradeOutOfSync.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()

    if (!isTradeOutOfSync) return

    BisqGap.V2()
    TradeOutOfSyncPaneContent(
        showReportToMediator = !isInMediation,
        onOpenChat = presenter::onOpenChat,
        onReportToMediator = headerPresenter::onOpenMediationConfirmationDialog,
    )
}

/**
 * Deliberately calm (no danger styling) and without a reject/cancel action: money may already be
 * in flight, and promoting cancel here would invite a scared user to abandon a recoverable trade.
 * The header keeps the reject action for users who deliberately look for it.
 */
@Composable
fun TradeOutOfSyncPaneContent(
    showReportToMediator: Boolean,
    onOpenChat: () -> Unit,
    onReportToMediator: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = BisqTheme.colors.dark_grey40)
                .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WarningIcon(modifier = Modifier.size(24.dp))
            BisqGap.HHalf()
            BisqText.BaseMedium(
                text = "mobile.bisqEasy.openTrades.outOfSync.headline".i18n(),
                color = BisqTheme.colors.warning,
            )
        }

        BisqGap.V2()

        BisqText.BaseRegular(
            text = "mobile.bisqEasy.openTrades.outOfSync.body".i18n(),
        )

        BisqGap.V2()

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqButton(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                text = "mobile.bisqEasy.openTrades.outOfSync.openChat".i18n(),
                onClick = onOpenChat,
                type = BisqButtonType.Outline,
                color = BisqTheme.colors.primary,
                borderColor = BisqTheme.colors.primary,
            )
            if (showReportToMediator) {
                BisqButton(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    text = "bisqEasy.openTrades.reportToMediator".i18n(),
                    onClick = onReportToMediator,
                    type = BisqButtonType.WarningOutline,
                )
            }
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeOutOfSyncPaneContent_Preview() {
    BisqTheme.Preview {
        TradeOutOfSyncPaneContent(
            showReportToMediator = true,
            onOpenChat = {},
            onReportToMediator = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeOutOfSyncPaneContent_InMediationPreview() {
    BisqTheme.Preview {
        TradeOutOfSyncPaneContent(
            showReportToMediator = false,
            onOpenChat = {},
            onReportToMediator = {},
        )
    }
}
