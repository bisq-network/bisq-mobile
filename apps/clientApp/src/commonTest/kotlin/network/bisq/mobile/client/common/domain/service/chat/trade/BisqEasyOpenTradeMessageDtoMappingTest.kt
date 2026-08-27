package network.bisq.mobile.client.common.domain.service.chat.trade

import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The transport type is client-only; the model is what both apps present. Two things this pins: the
 * `messageId` → `id` rename, and that `myUserProfile` / `chatReactions` come from outside the DTO —
 * the DTO carries neither in a form the model can use (its reactions are the unfiltered set).
 */
class BisqEasyOpenTradeMessageDtoMappingTest {
    private val me = createMockUserProfile("Bob")
    private val peer = createMockUserProfile("Alice")

    @Test
    fun `maps the dto fields onto the model`() {
        val citation = Citation(authorUserProfileId = me.id, text = "When do we settle?", chatMessageId = "msg-0")
        val dto = createDto(citation = citation, citationAuthorUserProfile = me)

        val message = dto.toDomain(myUserProfile = me, chatReactions = emptyList())

        assertEquals("msg-1", message.id)
        assertEquals(ChatMessageTypeEnum.TEXT, message.chatMessageType)
        assertEquals("Payment sent", message.text)
        assertEquals(citation, message.citation)
        assertEquals("Bob", message.citationAuthorUserName)
        assertEquals(1234567890000L, message.date)
        assertSame(peer, message.senderUserProfile)
        assertEquals("trade-1", message.tradeId)
        assertSame(peer, message.mediator)
        assertSame(dto.bisqEasyOffer, message.bisqEasyOffer)
    }

    /** `messageId` on the wire, `id` on the model — the model mirrors Bisq 2's field name. */
    @Test
    fun `messageId becomes the model id`() {
        val message = createDto().copy(messageId = "wire-id").toDomain(me, emptyList())

        assertEquals("wire-id", message.id)
    }

    @Test
    fun `my profile comes from the caller not the dto`() {
        val fromPeer = createDto().toDomain(myUserProfile = me, chatReactions = emptyList())
        assertFalse(fromPeer.isMyMessage)

        val fromMe = createDto(sender = me).toDomain(myUserProfile = me, chatReactions = emptyList())
        assertTrue(fromMe.isMyMessage)
    }

    /**
     * The caller passes the reactions it resolved for this message — the DTO's own
     * `chatMessageReactions` are not read, which is what lets the facade filter out removed ones.
     */
    @Test
    fun `reactions come from the caller not from the dto`() {
        val ignored = createReaction("dto-reaction")
        val resolved = createReaction("resolved-reaction")

        val message = createDto(reactions = setOf(ignored)).toDomain(me, listOf(resolved))

        assertEquals(listOf("resolved-reaction"), message.chatReactions.value.map { it.id })
    }

    private fun createDto(
        citation: Citation? = null,
        citationAuthorUserProfile: UserProfileVO? = null,
        sender: UserProfileVO = peer,
        reactions: Set<BisqEasyOpenTradeMessageReaction> = emptySet(),
    ) = BisqEasyOpenTradeMessageDto(
        tradeId = "trade-1",
        messageId = "msg-1",
        channelId = "channel-1",
        senderUserProfile = sender,
        receiverUserProfileId = me.id,
        receiverNetworkId = me.networkId,
        text = "Payment sent",
        citation = citation,
        date = 1234567890000L,
        mediator = peer,
        chatMessageType = ChatMessageTypeEnum.TEXT,
        bisqEasyOffer = null,
        chatMessageReactions = reactions,
        citationAuthorUserProfile = citationAuthorUserProfile,
    )

    private fun createReaction(id: String) =
        BisqEasyOpenTradeMessageReaction(
            id = id,
            senderUserProfile = peer,
            receiverUserProfileId = me.id,
            receiverNetworkId = me.networkId,
            chatChannelId = "channel-1",
            chatChannelDomain = ChatChannelDomainEnum.BISQ_EASY_OPEN_TRADES,
            chatMessageId = "msg-1",
            reactionId = ReactionEnum.THUMBS_UP.ordinal,
            date = 1234567890000L,
            isRemoved = false,
        )
}
