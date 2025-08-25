package network.bisq.mobile.presentation.ui.components.molecules.chat.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.LeaveChatIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun TradePeerLeftMessageBox(message: BisqEasyOpenTradeMessageModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(BisqTheme.colors.dark_grey30)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.padding(
                top = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPadding2X,
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPaddingHalf),
            )
            {
                LeaveChatIcon()
                val peerUserName = message.senderUserName
                BisqText.smallLight("bisqEasy.openTrades.chat.peerLeft.headline".i18n(peerUserName), color = BisqTheme.colors.primary)
            }
            BisqText.smallLight("bisqEasy.openTrades.chat.peerLeft.subHeadline".i18n())
            BisqText.xsmallLightGrey(message.dateString)
        }
    }
}