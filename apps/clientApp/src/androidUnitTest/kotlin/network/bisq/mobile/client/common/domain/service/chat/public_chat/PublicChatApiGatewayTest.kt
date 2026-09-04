package network.bisq.mobile.client.common.domain.service.chat.public_chat

import io.ktor.http.HttpStatusCode
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.di.clientJson
import network.bisq.mobile.client.common.domain.service.chat.trade.SendChatMessageReactionRequest
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the wire contract against bisq2's `PublicChatRestApi`. Paths above all: public chat hangs its
 * reactions off `/{channelId}/messages/{messageId}/reactions` while private chat uses
 * `/{channelId}/{messageId}/reactions`, and the two gateways are otherwise close enough to copy from
 * one to the other — where the only symptom would be a 404 at runtime.
 *
 * Bodies go through the injected [clientJson], not a bare `Json` — it sets `prettyPrint`, so a
 * substring match against compact JSON would pin a shape production never sends — and are decoded
 * back to the request type, which also catches a dropped field.
 */
class PublicChatApiGatewayTest {
    private val webSocketClientService: WebSocketClientService = mockk()
    private val webSocketApiClient = WebSocketApiClient(webSocketClientService, clientJson)
    private val gateway = PublicChatApiGateway(webSocketApiClient, webSocketClientService)

    /**
     * The sender travels in the body — a public channel has no identity of its own — and a reply's
     * citation must survive the trip: it is only ever exercised here, so dropping the field from the
     * request would otherwise go unnoticed until a quote arrives empty at the node.
     */
    @Test
    fun `sendTextMessage posts the text, the citation and the sender`() =
        runTest {
            val request = capturing()
            val citation = Citation(authorUserProfileId = "author-1", text = "quoted", chatMessageId = "m0")

            assertTrue(gateway.sendTextMessage(CHANNEL_ID, "hello", citation, "profile-1").isSuccess)

            assertEquals("POST", request.captured.method)
            assertEquals("$BASE/$CHANNEL_ID/messages", request.captured.path)
            assertEquals(
                SendPublicChatMessageRequest("hello", citation, "profile-1"),
                clientJson.decodeFromString(request.captured.body.orEmpty()),
            )
        }

    @Test
    fun `editMessage puts the new text at the message's own path`() =
        runTest {
            val request = capturing()

            assertTrue(gateway.editMessage(CHANNEL_ID, MESSAGE_ID, "fixed").isSuccess)

            assertEquals("PUT", request.captured.method)
            assertEquals("$BASE/$CHANNEL_ID/messages/$MESSAGE_ID", request.captured.path)
            assertEquals(
                EditPublicChatMessageRequest("fixed"),
                clientJson.decodeFromString(request.captured.body.orEmpty()),
            )
        }

    @Test
    fun `deleteMessage deletes the message's own path`() =
        runTest {
            val request = capturing()

            assertTrue(gateway.deleteMessage(CHANNEL_ID, MESSAGE_ID).isSuccess)

            assertEquals("DELETE", request.captured.method)
            assertEquals("$BASE/$CHANNEL_ID/messages/$MESSAGE_ID", request.captured.path)
        }

    @Test
    fun `addChatMessageReaction posts under the messages segment, unlike private chat`() =
        runTest {
            val request = capturing()

            assertTrue(gateway.addChatMessageReaction(CHANNEL_ID, MESSAGE_ID, ReactionEnum.THUMBS_UP, "profile-1").isSuccess)

            assertEquals("POST", request.captured.method)
            assertEquals("$BASE/$CHANNEL_ID/messages/$MESSAGE_ID/reactions", request.captured.path)
            assertEquals(
                SendChatMessageReactionRequest(ReactionEnum.THUMBS_UP.ordinal, false, "profile-1"),
                clientJson.decodeFromString(request.captured.body.orEmpty()),
            )
        }

    /**
     * The owner is named from the reaction, not from the selected identity: a public message carries
     * reactions from everyone, and bisq2 requires the owner on a removal.
     */
    @Test
    fun `removeChatMessageReaction names the reaction's own sender`() =
        runTest {
            val request = capturing()

            assertTrue(gateway.removeChatMessageReaction(CHANNEL_ID, MESSAGE_ID, reaction("owner-1")).isSuccess)

            assertEquals("$BASE/$CHANNEL_ID/messages/$MESSAGE_ID/reactions", request.captured.path)
            assertEquals(
                SendChatMessageReactionRequest(ReactionEnum.THUMBS_UP.ordinal, true, "owner-1"),
                clientJson.decodeFromString(request.captured.body.orEmpty()),
            )
        }

    @Test
    fun `consumeNotifications posts to the channel's consume endpoint`() =
        runTest {
            val request = capturing()

            assertTrue(gateway.consumeNotifications(CHANNEL_ID).isSuccess)

            assertEquals("POST", request.captured.method)
            assertEquals("$BASE/$CHANNEL_ID/consume-notifications", request.captured.path)
        }

    /**
     * The facade's three-way 403 discrimination rests on this seam: `WebSocketApiClient` unwraps a
     * JSON `{"error": …}` envelope to its bare token, while prose arrives verbatim.
     */
    @Test
    fun `an error envelope arrives as its bare token`() =
        runTest {
            capturing(statusCode = HttpStatusCode.Forbidden.value, body = """{"error":"permission_not_granted"}""")

            val exception = gateway.editMessage(CHANNEL_ID, MESSAGE_ID, "x").exceptionOrNull()

            assertIs<WebSocketRestApiException>(exception)
            assertEquals("permission_not_granted", exception.message)
        }

    /** A swapped topic here decodes as garbage at runtime, not at compile time. */
    @Test
    fun `each subscription names its own topic`() =
        runTest {
            val channels = WebSocketEventObserver()
            val messages = WebSocketEventObserver()
            val reactions = WebSocketEventObserver()
            coEvery { webSocketClientService.subscribe(Topic.PUBLIC_CHAT_CHANNELS, null) } returns channels
            coEvery { webSocketClientService.subscribe(Topic.PUBLIC_CHAT_MESSAGES, null) } returns messages
            coEvery { webSocketClientService.subscribe(Topic.PUBLIC_CHAT_REACTIONS, null) } returns reactions

            assertSame(channels, gateway.subscribeChannels())
            assertSame(messages, gateway.subscribeMessages())
            assertSame(reactions, gateway.subscribeReactions())
        }

    private fun capturing(
        statusCode: Int = HttpStatusCode.OK.value,
        body: String = "",
    ): CapturingSlot<WebSocketRestApiRequest> {
        val slot = slot<WebSocketRestApiRequest>()
        coEvery {
            webSocketClientService.sendRequestAndAwaitResponse(capture(slot))
        } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = statusCode, body = body)
        return slot
    }

    private fun reaction(ownerProfileId: String) =
        CommonPublicChatMessageReaction(
            id = "r1",
            userProfileId = ownerProfileId,
            chatChannelId = CHANNEL_ID,
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            chatMessageId = MESSAGE_ID,
            reactionId = ReactionEnum.THUMBS_UP.ordinal,
            date = 1L,
        )

    private companion object {
        const val BASE = "/api/v1/public-chat-channels"
        const val CHANNEL_ID = "discussion.bisq"
        const val MESSAGE_ID = "message-1"
    }
}
