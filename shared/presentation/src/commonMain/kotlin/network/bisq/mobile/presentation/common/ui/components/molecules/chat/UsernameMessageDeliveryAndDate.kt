package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun UsernameMessageDeliveryAndDate(
    message: BisqEasyOpenTradeMessageModel,
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
    messageDeliveryInfoByPeersProfileId: StateFlow<Map<String, MessageDeliveryInfoVO>>,
) {
    var showInfo by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .semantics(mergeDescendants = true) {}
                    .clickable(message.isMyMessage) { showInfo = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val date = @Composable {
                BisqText.XSmallLightGrey(
                    modifier =
                        Modifier
                            .widthIn(max = this@BoxWithConstraints.maxWidth * 0.4f),
                    text = message.dateString,
                )
            }
            val username = @Composable {
                BisqText.BaseRegular(
                    modifier =
                        Modifier
                            .widthIn(max = this@BoxWithConstraints.maxWidth * 0.6f),
                    text = message.senderUserName,
                )
            }

            if (message.isMyMessage) {
                Spacer(Modifier.weight(1f))
                date()
                Spacer(Modifier.width(BisqUIConstants.ScreenPaddingHalfQuarter))
                MessageDeliveryBox(
                    onResendMessage = onResendMessage,
                    userNameProvider = userNameProvider,
                    messageDeliveryInfoByPeersProfileId = messageDeliveryInfoByPeersProfileId,
                    showInfo,
                    onDismissMenu = {
                        showInfo = false
                    },
                )
                username()
            } else {
                username()
                Spacer(Modifier.width(BisqUIConstants.ScreenPaddingHalfQuarter))
                date()
            }
        }
    }
}

@Preview
@Composable
private fun UsernameMessageDeliveryAndDatePreview_MyMessagePreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob [Marvelously-Extraneous-Elephant-234345435]")
        val peerUserProfile =
            createMockUserProfile("Alice [Marvelously-Extraneous-Elephant-234345435]")

        val dto =
            BisqEasyOpenTradeMessageDto(
                tradeId = "trade123",
                messageId = "msg123",
                channelId = "channel123",
                senderUserProfile = myUserProfile,
                receiverUserProfileId = peerUserProfile.networkId.pubKey.id,
                receiverNetworkId = peerUserProfile.networkId,
                text = "Hello!",
                citation = null,
                date = 1234567890000L,
                mediator = null,
                chatMessageType = ChatMessageTypeEnum.TEXT,
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

        UsernameMessageDeliveryAndDate(
            message = message,
            onResendMessage = {},
            userNameProvider = { it },
            messageDeliveryInfoByPeersProfileId = MutableStateFlow(emptyMap()),
        )
    }
}

@Preview
@Composable
private fun UsernameMessageDeliveryAndDatePreview_PeerMessagePreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob [Marvelously-Extraneous-Elephant-234345435]")
        val peerUserProfile =
            createMockUserProfile("Alice")

        val dto =
            BisqEasyOpenTradeMessageDto(
                tradeId = "trade123",
                messageId = "msg456",
                channelId = "channel123",
                senderUserProfile = peerUserProfile,
                receiverUserProfileId = myUserProfile.networkId.pubKey.id,
                receiverNetworkId = myUserProfile.networkId,
                text = "Hi there!",
                citation = null,
                date = 1234567890000L,
                mediator = null,
                chatMessageType = ChatMessageTypeEnum.TEXT,
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

        UsernameMessageDeliveryAndDate(
            message = message,
            onResendMessage = {},
            userNameProvider = { it },
            messageDeliveryInfoByPeersProfileId = MutableStateFlow(emptyMap()),
        )
    }
}
