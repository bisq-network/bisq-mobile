package network.bisq.mobile.client.common.domain.service.chat.private_chat

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatNotPermittedException
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientPrivateChatServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: PrivateChatApiGateway = mockk(relaxed = true)
    private val globalUiManager: GlobalUiManager = mockk(relaxed = true)
    private val capabilities = MutableStateFlow(BackendCapabilities(setOf(Feature.PRIVATE_CHAT.key)))
    private val backendCapabilitiesService: BackendCapabilitiesService =
        mockk { every { this@mockk.capabilities } returns this@ClientPrivateChatServiceFacadeTest.capabilities }
    private val json = Json { ignoreUnknownKeys = true }

    private val channelsObserver = WebSocketEventObserver()
    private val messagesObserver = WebSocketEventObserver()
    private val reactionsObserver = WebSocketEventObserver()

    private val me: UserProfileVO = createMockUserProfile("me")
    private val peer: UserProfileVO = createMockUserProfile("peer")

    private var channelSequence = 0
    private var messageSequence = 0
    private var reactionSequence = 0

    private lateinit var facade: ClientPrivateChatServiceFacade

    override fun onSetup() {
        coEvery { apiGateway.subscribeChannels() } returns channelsObserver
        coEvery { apiGateway.subscribeMessages() } returns messagesObserver
        coEvery { apiGateway.subscribeReactions() } returns reactionsObserver
        facade = ClientPrivateChatServiceFacade(apiGateway, backendCapabilitiesService, json, globalUiManager, testDispatcher)
    }

    @Test
    fun `is unsupported and subscribes to nothing when the node does not advertise the feature`() =
        runTest {
            capabilities.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))

            assertFalse(facade.isSupported.first())

            facade.activate()
            advanceUntilIdle()

            coVerify(exactly = 0) { apiGateway.subscribeChannels() }
            coVerify(exactly = 0) { apiGateway.subscribeMessages() }
            coVerify(exactly = 0) { apiGateway.subscribeReactions() }
            assertTrue(facade.channels.value.isEmpty())
        }

    /**
     * The ordering this facade actually runs in: `ClientApplicationLifecycleService` activates it
     * before `ConfigServiceFacade`, so the capability manifest has not been fetched yet and the
     * capability set is still the legacy baseline. Reading `isSupported` once at activation left the
     * feature dead for the whole session — every other test here seeds the capability first and so
     * never exercised this.
     */
    @Test
    fun `subscribes once the node advertises the feature, even though it was unsupported at activate`() =
        runTest {
            capabilities.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeChannels() }

            capabilities.value = BackendCapabilities(setOf(Feature.PRIVATE_CHAT.key))
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeChannels() }
            coVerify(exactly = 1) { apiGateway.subscribeMessages() }
            coVerify(exactly = 1) { apiGateway.subscribeReactions() }
        }

    /**
     * The node advertises `private-chat` from `/config/capabilities`, which is public and not
     * permission-filtered, so a pairing without `PRIVATE_CHAT_CHANNELS` reaches this call with
     * [ClientPrivateChatServiceFacade.isSupported] true. Presenters live in `:shared:presentation`
     * and cannot see the HTTP types, so the distinction has to be made here or not at all.
     */
    @Test
    fun `a forbidden channel creation is reported as a withheld permission, not a generic failure`() =
        runTest {
            coEvery { apiGateway.findOrCreateChannel(any()) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.Forbidden, "Forbidden"))

            val result = facade.findOrCreateChannel("peer-profile-id")

            assertTrue(result.exceptionOrNull() is PrivateChatNotPermittedException)
        }

    @Test
    fun `other channel creation failures keep their original cause`() =
        runTest {
            val cause = WebSocketRestApiException(HttpStatusCode.NotFound, "No user profile found")
            coEvery { apiGateway.findOrCreateChannel(any()) } returns Result.failure(cause)

            val result = facade.findOrCreateChannel("peer-profile-id")

            assertTrue(result.exceptionOrNull() === cause, "only 403 means the permission was withheld")
        }

    @Test
    fun `a channel payload becomes a channel model carrying its unread count`() =
        runTest {
            activateAndSettle()

            emitChannel(unreadCount = 3)
            advanceUntilIdle()

            val channel = facade.channels.value.single()
            assertEquals(CHANNEL_ID, channel.id)
            assertEquals("peer", channel.peer.userName)
            assertEquals(3L, channel.unreadCount.value)
        }

    @Test
    fun `a re-sent channel updates the existing model rather than replacing it`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 3)
            emitMessage(messageId = "message-1", text = "hi")
            advanceUntilIdle()
            val model = facade.channels.value.single()

            emitChannel(unreadCount = 0)
            advanceUntilIdle()

            assertTrue(model === facade.channels.value.single(), "the channel model must survive an unread-count update")
            assertEquals(0L, model.unreadCount.value)
            assertEquals(1, model.chatMessages.value.size)
        }

    @Test
    fun `messages and reactions merge into the channel regardless of arrival order`() =
        runTest {
            activateAndSettle()

            // Reaction first: it references a message the client has not seen yet.
            emitReaction(messageId = "message-1", reactionId = 1)
            emitMessage(messageId = "message-1", text = "hi")
            emitChannel(unreadCount = 1)
            advanceUntilIdle()

            val message =
                facade.channels.value
                    .single()
                    .chatMessages.value
                    .single()
            assertEquals("hi", message.text)
            assertEquals(listOf(1), message.chatReactions.value.map { it.reactionId })
        }

    @Test
    fun `a removed reaction arriving with a fresh id still removes the original`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 0)
            emitMessage(messageId = "message-1", text = "hi")
            emitReaction(messageId = "message-1", reactionId = 1, id = "reaction-1")
            advanceUntilIdle()

            emitReaction(messageId = "message-1", reactionId = 1, id = "reaction-2", isRemoved = true)
            advanceUntilIdle()

            val message =
                facade.channels.value
                    .single()
                    .chatMessages.value
                    .single()
            assertTrue(message.chatReactions.value.isEmpty())
        }

    @Test
    fun `a channel removed by the node is dropped instead of being re-added`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 0)
            advanceUntilIdle()

            emitChannel(unreadCount = 0, modificationType = ModificationType.REMOVED)
            advanceUntilIdle()

            assertTrue(facade.channels.value.isEmpty())
        }

    /**
     * REPLACE never comes from the node, which only sends ADDED/REMOVED on these topics. It is the
     * snapshot `WebSocketClientImpl` synthesises from the subscription response, and
     * `WebSocketClientService` re-applies the subscription on every reconnect — so this is what a
     * client sees after being offline while the peer list changed elsewhere.
     */
    @Test
    fun `a resubscribe snapshot drops a channel that was left while this client was offline`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 0)
            advanceUntilIdle()
            assertEquals(1, facade.channels.value.size)

            emitChannelSnapshot(emptyList())
            advanceUntilIdle()

            assertTrue(facade.channels.value.isEmpty(), "the snapshot is authoritative about absence")
        }

    @Test
    fun `a resubscribe snapshot keeps the channels it still lists`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 3)
            advanceUntilIdle()

            emitChannelSnapshot(listOf(CHANNEL_ID))
            advanceUntilIdle()

            assertEquals(
                CHANNEL_ID,
                facade.channels.value
                    .single()
                    .id,
            )
        }

    /** The node filters removed reactions out of the snapshot, so an absent one is a withdrawn one. */
    @Test
    fun `a resubscribe snapshot drops a reaction withdrawn while this client was offline`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 0)
            emitMessage(messageId = "message-1", text = "hi")
            emitReaction(messageId = "message-1", reactionId = 1)
            advanceUntilIdle()
            assertEquals(1, singleMessageReactions().size)

            emitReactionSnapshot()
            advanceUntilIdle()

            assertTrue(singleMessageReactions().isEmpty())
        }

    /**
     * The contract in `PrivateChatServiceFacade` is that a pairing without the permission is refused
     * *every* call, not only channel creation — and a pairing that lost it still reaches this screen,
     * because the DMs keep arriving over the `PRIVATE_CHAT_*` topics, which bisq 2 never authorises.
     * Opening an existing conversation therefore skips `findOrCreateChannel` entirely, so the first
     * send used to be the first 403 and it surfaced as a generic connection error.
     *
     * All six routes sit under `private-chat-channels`, which bisq 2 maps to `PRIVATE_CHAT_CHANNELS`
     * for every method (`RestPermissionMapping`), so one 403 stands for all of them.
     */
    @Test
    fun `a forbidden private-chat call is reported as a withheld permission, whichever call it is`() =
        runTest {
            val forbidden = { Result.failure<Unit>(WebSocketRestApiException(HttpStatusCode.Forbidden, "permission_not_granted")) }
            coEvery { apiGateway.sendTextMessage(any(), any(), any()) } answers { forbidden() }
            coEvery { apiGateway.addChatMessageReaction(any(), any(), any()) } answers { forbidden() }
            coEvery { apiGateway.leaveChannel(any()) } answers { forbidden() }

            val results =
                listOf(
                    "sendChatMessage" to facade.sendChatMessage(CHANNEL_ID, "hello", null),
                    "addChatMessageReaction" to facade.addChatMessageReaction(CHANNEL_ID, "m1", ReactionEnum.THUMBS_UP),
                    "leaveChannel" to facade.leaveChannel(CHANNEL_ID),
                )

            results.forEach { (name, result) ->
                assertTrue(
                    result.exceptionOrNull() is PrivateChatNotPermittedException,
                    "$name must translate the 403 like findOrCreateChannel does",
                )
            }
        }

    /** The other direction, so the translation cannot widen into "any failure is a permission problem". */
    @Test
    fun `other private-chat failures keep their original cause`() =
        runTest {
            val cause = WebSocketRestApiException(HttpStatusCode.NotFound, "No channel")
            coEvery { apiGateway.sendTextMessage(any(), any(), any()) } returns Result.failure(cause)

            val result = facade.sendChatMessage(CHANNEL_ID, "hello", null)

            assertTrue(result.exceptionOrNull() === cause, "only 403 means the permission was withheld")
        }

    private fun singleMessageReactions() =
        facade.channels.value
            .single()
            .chatMessages.value
            .single()
            .chatReactions.value

    private suspend fun emitChannelSnapshot(channelIds: List<String>) {
        val dtos =
            channelIds.map { channelId ->
                TwoPartyPrivateChatChannelDto(
                    id = channelId,
                    chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
                    peer = peer,
                    myUserProfile = me,
                    unreadCount = 0,
                )
            }
        channelsObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PRIVATE_CHAT_CHANNELS,
                subscriberId = "channels",
                deferredPayload = json.encodeToString(dtos),
                modificationType = ModificationType.REPLACE,
                sequenceNumber = channelSequence++,
            ),
        )
    }

    private suspend fun emitReactionSnapshot() {
        reactionsObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PRIVATE_CHAT_REACTIONS,
                subscriberId = "reactions",
                deferredPayload = json.encodeToString(emptyList<TwoPartyPrivateChatMessageReactionDto>()),
                modificationType = ModificationType.REPLACE,
                sequenceNumber = reactionSequence++,
            ),
        )
    }

    private suspend fun TestScope.activateAndSettle() {
        facade.activate()
        advanceUntilIdle()
    }

    private suspend fun emitChannel(
        unreadCount: Long,
        modificationType: ModificationType = ModificationType.ADDED,
    ) {
        val dto =
            TwoPartyPrivateChatChannelDto(
                id = CHANNEL_ID,
                chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
                peer = peer,
                myUserProfile = me,
                unreadCount = unreadCount,
            )
        channelsObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PRIVATE_CHAT_CHANNELS,
                subscriberId = "channels",
                deferredPayload = json.encodeToString(listOf(dto)),
                modificationType = modificationType,
                sequenceNumber = channelSequence++,
            ),
        )
    }

    private suspend fun emitMessage(
        messageId: String,
        text: String,
    ) {
        val dto =
            TwoPartyPrivateChatMessageDto(
                messageId = messageId,
                channelId = CHANNEL_ID,
                senderUserProfile = peer,
                receiverUserProfileId = "receiver-1",
                receiverNetworkId = me.networkId,
                text = text,
                citation = null,
                date = 1234L,
                chatMessageType = ChatMessageTypeEnum.TEXT,
                chatMessageReactions = emptySet(),
                citationAuthorUserProfile = null,
            )
        messagesObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PRIVATE_CHAT_MESSAGES,
                subscriberId = "messages",
                deferredPayload = json.encodeToString(listOf(dto)),
                modificationType = ModificationType.ADDED,
                sequenceNumber = messageSequence++,
            ),
        )
    }

    private suspend fun emitReaction(
        messageId: String,
        reactionId: Int,
        id: String = "reaction-$reactionId",
        isRemoved: Boolean = false,
    ) {
        val dto =
            TwoPartyPrivateChatMessageReactionDto(
                id = id,
                senderUserProfile = peer,
                receiverUserProfileId = "receiver-1",
                receiverNetworkId = me.networkId,
                chatChannelId = CHANNEL_ID,
                chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
                chatMessageId = messageId,
                reactionId = reactionId,
                date = 1234L,
                isRemoved = isRemoved,
            )
        reactionsObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PRIVATE_CHAT_REACTIONS,
                subscriberId = "reactions",
                deferredPayload = json.encodeToString(listOf(dto)),
                modificationType = if (isRemoved) ModificationType.REMOVED else ModificationType.ADDED,
                sequenceNumber = reactionSequence++,
            ),
        )
    }

    private companion object {
        const val CHANNEL_ID = "discussion.a-b"
    }
}
