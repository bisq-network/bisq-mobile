package network.bisq.mobile.data.replicated.chat.two_party

import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The DM counterpart of [network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelTest].
 *
 * The identity rule is the point of these tests: a message keeps its id for life while its reactions
 * and delivery status move, so a plain `Set` would keep both copies and the UI would render the
 * message twice — once with the old reactions.
 */
class TwoPartyPrivateChatChannelTest {
    private val me = createMockUserProfile("Bob")
    private val peer = createMockUserProfile("Alice")

    @Test
    fun `a message re-added with the same id replaces the old copy`() {
        val channel = createChannel()
        val original = createMessage(messageId = "message-1", reactionId = 1)
        val updated = createMessage(messageId = "message-1", reactionId = 2)

        channel.addChatMessage(original)
        channel.addChatMessage(updated)

        val stored = channel.chatMessages.value.single()
        assertSame(updated, stored)
        assertEquals(
            2,
            stored.chatReactions.value
                .single()
                .reactionId,
        )
    }

    @Test
    fun `messages with different ids all stay`() {
        val channel = createChannel()

        channel.addChatMessage(createMessage(messageId = "message-1", reactionId = 1))
        channel.addChatMessage(createMessage(messageId = "message-2", reactionId = 1))

        assertEquals(
            setOf("message-1", "message-2"),
            channel.chatMessages.value
                .map { it.id }
                .toSet(),
        )
    }

    @Test
    fun `a reaction I sent is recognised as mine on the replacing copy`() {
        val channel = createChannel()
        channel.addChatMessage(createMessage(messageId = "message-1", reactionId = 1))
        channel.addChatMessage(createMessage(messageId = "message-1", reactionId = 2, reactionSender = me))

        val stored = channel.chatMessages.value.single()
        assertTrue(stored.isMyChatReaction(stored.chatReactions.value.single()))
    }

    @Test
    fun `setAllChatMessages keeps only the last copy of a duplicated id`() {
        val channel = createChannel()
        val original = createMessage(messageId = "message-1", reactionId = 1)
        val updated = createMessage(messageId = "message-1", reactionId = 2)

        channel.setAllChatMessages(setOf(original, updated))

        assertSame(updated, channel.chatMessages.value.single())
    }

    @Test
    fun `unread count is whatever was last set`() {
        val channel = createChannel()

        channel.setUnreadCount(3L)

        assertEquals(3L, channel.unreadCount.value)
    }

    private fun createChannel(): TwoPartyPrivateChatChannel =
        TwoPartyPrivateChatChannel(
            id = "channel-1",
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            peer = peer,
            myUserProfile = me,
        )

    private fun createMessage(
        messageId: String,
        reactionId: Int,
        reactionSender: UserProfileVO = peer,
    ): TwoPartyPrivateChatMessage =
        createMockTwoPartyPrivateChatMessage(
            id = messageId,
            text = "hello",
            date = 1234L,
            senderUserProfile = peer,
            myUserProfile = me,
            chatReactions =
                listOf(
                    TwoPartyPrivateChatMessageReaction(
                        id = "reaction-$reactionId",
                        senderUserProfile = reactionSender,
                        receiverUserProfileId = "receiver-1",
                        receiverNetworkId = me.networkId,
                        chatChannelId = "channel-1",
                        chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
                        chatMessageId = messageId,
                        reactionId = reactionId,
                        date = 1234L,
                        isRemoved = false,
                    ),
                ),
        )
}
