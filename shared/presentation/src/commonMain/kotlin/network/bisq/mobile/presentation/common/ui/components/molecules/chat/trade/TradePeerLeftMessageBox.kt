package network.bisq.mobile.presentation.common.ui.components.molecules.chat.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LeaveChatIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun TradePeerLeftMessageBox(
    message: BisqEasyOpenTradeMessageModel,
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
                val peerUserName = message.senderUserName
                BisqText.SmallLight("bisqEasy.openTrades.chat.peerLeft.headline".i18n(peerUserName), color = BisqTheme.colors.primary)
            }
            BisqText.SmallLight("bisqEasy.openTrades.chat.peerLeft.subHeadline".i18n())
            BisqText.XSmallLightGrey(message.dateString)
        }
    }
}

@Preview
@Composable
private fun TradePeerLeftMessageBoxPreview() {
    BisqTheme.Preview {
        val peerUserProfile = createMockUserProfile("Alice")
        val myUserProfile = createMockUserProfile("Bob")

        val dto =
            BisqEasyOpenTradeMessageDto(
                tradeId = "trade123",
                messageId = "msg123",
                channelId = "channel123",
                senderUserProfile = peerUserProfile,
                receiverUserProfileId = myUserProfile.networkId.pubKey.id,
                receiverNetworkId = myUserProfile.networkId,
                text = null,
                citation = null,
                date = 1234567890000L,
                mediator = null,
                chatMessageType = ChatMessageTypeEnum.LEAVE,
                bisqEasyOffer = null,
                chatMessageReactions = emptySet(),
                citationAuthorUserProfile = null,
            )

        val message =
            BisqEasyOpenTradeMessageModel(
                dto,
                myUserProfile,
                emptyList(),
            )

        TradePeerLeftMessageBox(message = message)
    }
}
