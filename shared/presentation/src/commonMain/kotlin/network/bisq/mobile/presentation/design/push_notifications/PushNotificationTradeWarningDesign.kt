package network.bisq.mobile.presentation.design.push_notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Design POC: Visual indicators when relayed push notifications are OFF.
 *
 * Three warning surfaces:
 *
 * 1. Trade screen inline banner: non-dismissible warning above trade actions when
 *    the user has an active trade and relay is disabled. Links to Settings.
 *
 * 2. Trade list indicator: small warning icon next to open trades header when
 *    relay is off and open trades exist.
 *
 * 3. Settings section note: warning-colored text below the toggle when OFF
 *    (included in PushNotificationSettingsDesign.kt).
 */

@Composable
private fun TradeScreenWarningBanner(onEnableClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = BisqTheme.colors.warning.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WarningIcon(modifier = Modifier.size(18.dp))
        BisqText.SmallLight(
            text = "You may miss trade updates when the app is closed",
            color = BisqTheme.colors.warning,
            modifier = Modifier.weight(1f),
        )
        BisqText.SmallLight(
            text = "Enable \u2192",
            color = BisqTheme.colors.primary,
            modifier = Modifier.clickable { onEnableClick() },
        )
    }
}

@Composable
private fun OpenTradesHeaderWithWarning(tradeCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BisqText.H5Light("Open Trades ($tradeCount)")
        WarningIcon(modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SimulatedTradeItem(
    tradeId: String,
    status: String,
    amount: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = BisqTheme.colors.dark_grey30,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            BisqText.BaseLight(tradeId)
            BisqGap.VQuarter()
            BisqText.SmallLight(
                text = status,
                color = BisqTheme.colors.mid_grey20,
            )
        }
        BisqText.BaseLight(amount)
    }
}

@Composable
private fun SimulatedTradeScreen(
    tradeId: String,
    showWarningBanner: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqText.H4Light("Trade Details")
        BisqGap.VHalf()

        if (showWarningBanner) {
            TradeScreenWarningBanner(onEnableClick = {})
            BisqGap.VHalf()
        }

        BisqText.BaseLight("Trade ID: $tradeId")
        BisqText.SmallLight(
            text = "Status: Waiting for payment",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.V1()

        BisqText.BaseLight("Amount: 0.005 BTC")
        BisqText.BaseLight("Price: 68,420.00 USD")

        BisqGap.V1()
        BisqHDivider()
        BisqGap.V1()

        BisqText.SmallLight(
            text = "Chat with counterparty",
            color = BisqTheme.colors.primary,
        )
    }
}

@Composable
private fun SimulatedTradeListScreen(
    showWarningIcon: Boolean,
    showBanner: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.backgroundColor)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        if (showWarningIcon) {
            OpenTradesHeaderWithWarning(tradeCount = 2)
        } else {
            BisqText.H5Light("Open Trades (2)")
        }

        if (showBanner) {
            TradeScreenWarningBanner(onEnableClick = {})
        }

        BisqGap.VHalf()

        SimulatedTradeItem(
            tradeId = "Trade #abc-123",
            status = "Waiting for payment",
            amount = "0.005 BTC",
        )
        SimulatedTradeItem(
            tradeId = "Trade #def-456",
            status = "Payment sent",
            amount = "0.012 BTC",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeScreen_WithWarning_Preview() {
    BisqTheme.Preview {
        SimulatedTradeScreen(
            tradeId = "abc-123-def",
            showWarningBanner = true,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeScreen_NoWarning_Preview() {
    BisqTheme.Preview {
        SimulatedTradeScreen(
            tradeId = "abc-123-def",
            showWarningBanner = false,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeList_WithWarningIcon_Preview() {
    BisqTheme.Preview {
        SimulatedTradeListScreen(
            showWarningIcon = true,
            showBanner = true,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeList_NoWarning_Preview() {
    BisqTheme.Preview {
        SimulatedTradeListScreen(
            showWarningIcon = false,
            showBanner = false,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WarningBanner_Standalone_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Inline warning banner (not dismissible):",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            TradeScreenWarningBanner(onEnableClick = {})
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun Comparison_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Trade list header \u2014 without warning:",
                color = BisqTheme.colors.light_grey10,
            )
            BisqText.H5Light("Open Trades (2)")

            BisqText.SmallLight(
                "Trade list header \u2014 with warning:",
                color = BisqTheme.colors.light_grey10,
            )
            OpenTradesHeaderWithWarning(tradeCount = 2)
        }
    }
}
