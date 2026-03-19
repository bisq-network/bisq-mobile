package network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades

import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BisqEasyOpenTradeMessageModelTest {
    @Test
    fun equalIdsAreTreatedAsSameLogicalMessage() {
        val myUserProfile = createMockUserProfile("me")
        val sender = createMockUserProfile("sender")

        val first =
            BisqEasyOpenTradeMessageModel(
                bisqEasyOpenTradeMessage =
                    createMessageDto(
                        sender = sender,
                        messageId = "message-1",
                        text = "first",
                    ),
                myUserProfile = myUserProfile,
                chatReactions = emptyList(),
            )
        val second =
            BisqEasyOpenTradeMessageModel(
                bisqEasyOpenTradeMessage =
                    createMessageDto(
                        sender = sender,
                        messageId = "message-1",
                        text = "second",
                    ),
                myUserProfile = myUserProfile,
                chatReactions = emptyList(),
            )

        assertEquals(first, second)
        assertEquals(1, setOf(first, second).size)
    }

    @Test
    fun differentIdsRemainDistinctMessages() {
        val myUserProfile = createMockUserProfile("me")
        val sender = createMockUserProfile("sender")

        val first =
            BisqEasyOpenTradeMessageModel(
                bisqEasyOpenTradeMessage = createMessageDto(sender = sender, messageId = "message-1"),
                myUserProfile = myUserProfile,
                chatReactions = emptyList(),
            )
        val second =
            BisqEasyOpenTradeMessageModel(
                bisqEasyOpenTradeMessage = createMessageDto(sender = sender, messageId = "message-2"),
                myUserProfile = myUserProfile,
                chatReactions = emptyList(),
            )

        assertNotEquals(first, second)
        assertEquals(2, setOf(first, second).size)
    }

    private fun createMessageDto(
        sender: network.bisq.mobile.data.replicated.user.profile.UserProfileVO,
        messageId: String,
        text: String? = "hello",
    ): BisqEasyOpenTradeMessageDto =
        BisqEasyOpenTradeMessageDto(
            tradeId = "trade-1",
            messageId = messageId,
            channelId = "channel-1",
            senderUserProfile = sender,
            receiverUserProfileId = "receiver-1",
            receiverNetworkId = sender.networkId,
            text = text,
            citation = null,
            date = 1234L,
            mediator = null,
            chatMessageType = ChatMessageTypeEnum.TEXT,
            bisqEasyOffer = null,
            chatMessageReactions = emptySet(),
            citationAuthorUserProfile = null,
        )
}
