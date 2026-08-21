package network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.data.replicated.chat.priv.PrivateChatMessage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LeaveChatIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * The DM counterpart of `TradePeerLeftMessageBox`. Kept separate rather than reusing it because that
 * one says "has left the trade" and adds a mediator sub-headline, neither of which applies here.
 */
@Composable
fun PrivateChatPeerLeftMessageBox(
    message: PrivateChatMessage<*>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(BisqTheme.colors.dark_grey30)
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    top = BisqUIConstants.ScreenPadding,
                    bottom = BisqUIConstants.ScreenPadding2X,
                    start = BisqUIConstants.ScreenPadding,
                    end = BisqUIConstants.ScreenPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPaddingHalf),
            ) {
                LeaveChatIcon()
                BisqText.SmallLight(
                    "mobile.privateChats.chat.peerLeft".i18n(message.senderUserName),
                    color = BisqTheme.colors.primary,
                )
            }
            BisqText.XSmallLightGrey(message.dateString)
        }
    }
}
