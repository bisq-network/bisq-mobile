package network.bisq.mobile.client.common.domain.service.chat.trade

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the TRADE_CHAT_MESSAGES and CHAT_REACTIONS subscription collectors of
 * [ClientTradeChatMessagesServiceFacade]: both stay gated on the first open trade, then decode
 * and apply events. Empty-list payloads keep the assertions on the wiring — DTO-to-domain mapping
 * is pinned by [BisqEasyOpenTradeMessageDtoMappingTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientTradeChatMessagesServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val apiGateway: TradeChatMessagesApiGateway = mockk(relaxed = true)
    private val globalUiManager: GlobalUiManager = mockk(relaxed = true)
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val failedSubscriptionTopics = MutableStateFlow<Set<Topic>>(emptySet())
    private val json = Json { ignoreUnknownKeys = true }
    private val openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    private val selectedUserProfile = MutableStateFlow<UserProfileVO?>(null)
    private val tradeChatsObserver = WebSocketEventObserver()
    private val chatReactionsObserver = WebSocketEventObserver()
    private lateinit var facade: ClientTradeChatMessagesServiceFacade

    private companion object {
        const val TRADE_ID = "trade-1"
    }

    override fun onSetup() {
        every { tradesServiceFacade.openTradeItems } returns openTradeItems
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)
        every { userProfileServiceFacade.selectedUserProfile } returns selectedUserProfile
        every { webSocketClientService.failedSubscriptionTopics } returns failedSubscriptionTopics
        coEvery { apiGateway.subscribeTradeChats() } returns tradeChatsObserver
        coEvery { apiGateway.subscribeChatReactions() } returns chatReactionsObserver
        facade =
            ClientTradeChatMessagesServiceFacade(
                tradesServiceFacade,
                userProfileServiceFacade,
                apiGateway,
                json,
                globalUiManager,
                webSocketClientService,
                StandardTestDispatcherProvider(testDispatcher),
            )
    }

    /** A subscribe that failed is only retried on the next reconnect, so a wait on the sync has to be told. */
    @Test
    fun `a failed chat subscription is reported until a reconnect clears it`() =
        runTest {
            facade.activate()
            advanceUntilIdle()
            assertFalse(facade.chatMessagesSyncFailed.value)

            failedSubscriptionTopics.value = setOf(Topic.TRADE_CHAT_MESSAGES)
            advanceUntilIdle()
            assertTrue(facade.chatMessagesSyncFailed.value)

            failedSubscriptionTopics.value = emptySet()
            advanceUntilIdle()
            assertFalse(facade.chatMessagesSyncFailed.value)

            facade.deactivate()
        }

    private var dispatchersMocked = false

    /**
     * Routes the profile collector, which runs on Dispatchers.Default in production, onto the test
     * dispatcher so the synced flag it drives can be asserted on. Base already set Main. Only for
     * the tests that need it, and after every coEvery/coVerify of the test: a suspend recording made
     * while Dispatchers is mocked picks up the Dispatchers.Default read its coroutine runner makes.
     */
    private fun routeDefaultDispatcherOntoTestDispatcher() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Default } returns testDispatcher
        dispatchersMocked = true
    }

    override fun onTearDown() {
        try {
            if (dispatchersMocked) unmockkStatic(Dispatchers::class)
        } finally {
            super.onTearDown()
        }
    }

    /**
     * On a cold start over Tor the snapshot can land before the user profile, and without the profile
     * it cannot be applied to any channel, so an empty channel still says nothing until both are in.
     */
    @Test
    fun `chat messages sync once both the snapshot and the user profile are in`() =
        runTest {
            routeDefaultDispatcherOntoTestDispatcher()
            facade.activate()
            openTradeItems.value = listOf(mockk(relaxed = true))
            advanceUntilIdle()

            tradeChatsObserver.setEvent(tradeChatSnapshot())
            advanceUntilIdle()
            assertFalse(facade.chatMessagesSynced.value, "The snapshot could not be applied without the profile")

            selectedUserProfile.value = mockk()
            advanceUntilIdle()
            assertTrue(facade.chatMessagesSynced.value)

            facade.deactivate()
            assertFalse(facade.chatMessagesSynced.value)
        }

    @Test
    fun `chat messages sync when the snapshot lands after the user profile`() =
        runTest {
            selectedUserProfile.value = mockk()
            routeDefaultDispatcherOntoTestDispatcher()
            facade.activate()
            openTradeItems.value = listOf(mockk(relaxed = true))
            advanceUntilIdle()
            assertFalse(facade.chatMessagesSynced.value, "Nothing has been delivered yet")

            tradeChatsObserver.setEvent(tradeChatSnapshot())
            advanceUntilIdle()

            assertTrue(facade.chatMessagesSynced.value)
            facade.deactivate()
        }

    @Test
    fun `trade chats subscription waits for the first open trade and then collects events`() =
        runTest {
            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeTradeChats() }

            openTradeItems.value = listOf(mockk(relaxed = true))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeTradeChats() }

            tradeChatsObserver.setEvent(tradeChatSnapshot())
            advanceUntilIdle()

            facade.deactivate()
        }

    /**
     * After a reconnect the TRADES and TRADE_CHAT_MESSAGES snapshots are requested together, so the chat
     * messages of a trade can land before the trade itself. The trade's chat must still show them.
     */
    @Test
    fun `chat messages that land before their trade fill its chat once the trade arrives`() =
        runTest {
            selectedUserProfile.value = createMockUserProfile("me")
            facade.activate()
            openTradeItems.value = listOf(openTrade("other-trade"))
            advanceUntilIdle()
            tradeChatsObserver.setEvent(tradeChatSnapshot(chatMessage(TRADE_ID)))
            advanceUntilIdle()

            val trade = openTrade(TRADE_ID)
            openTradeItems.value = openTradeItems.value + trade
            advanceUntilIdle()

            verify { trade.bisqEasyOpenTradeChannelModel.setAllChatMessages(match { it.size == 1 }) }
            facade.deactivate()
        }

    /**
     * On a cold start the chat snapshot can land before the user profile. Each trade's chat is mapped with that
     * trade's own profile, so every open trade's chat is filled with the snapshot already, before synced reads
     * true; synced itself still waits for the selected profile.
     */
    @Test
    fun `chats fill with the snapshot before the user profile lands and synced waits for it`() =
        runTest {
            routeDefaultDispatcherOntoTestDispatcher()
            facade.activate()
            val first = openTrade(TRADE_ID)
            val second = openTrade("trade-2")
            openTradeItems.value = listOf(first, second)
            advanceUntilIdle()
            tradeChatsObserver.setEvent(tradeChatSnapshot(chatMessage(TRADE_ID), chatMessage("trade-2")))
            advanceUntilIdle()

            verify { first.bisqEasyOpenTradeChannelModel.setAllChatMessages(match { it.size == 1 }) }
            verify { second.bisqEasyOpenTradeChannelModel.setAllChatMessages(match { it.size == 1 }) }
            assertFalse(facade.chatMessagesSynced.value)

            selectedUserProfile.value = createMockUserProfile("me")
            advanceUntilIdle()

            assertTrue(facade.chatMessagesSynced.value)
            facade.deactivate()
        }

    /**
     * Open trades can belong to different profiles. A message the trade's own profile sent stays this user's
     * message while another profile is selected; read as a peer message it would also notify.
     */
    @Test
    fun `a trade's messages keep their owner while another profile is selected`() =
        runTest {
            val alice = createMockUserProfile("alice")
            selectedUserProfile.value = createMockUserProfile("bob")
            facade.activate()
            val trade = openTrade(TRADE_ID, myUserProfile = alice)
            openTradeItems.value = listOf(trade)
            advanceUntilIdle()
            val published = slot<Set<BisqEasyOpenTradeMessage>>()
            every { trade.bisqEasyOpenTradeChannelModel.setAllChatMessages(capture(published)) } just Runs

            tradeChatsObserver.setEvent(tradeChatSnapshot(chatMessage(TRADE_ID, sender = alice)))
            advanceUntilIdle()
            selectedUserProfile.value = createMockUserProfile("carol")
            advanceUntilIdle()

            assertTrue(published.captured.single().isMyMessage)
            facade.deactivate()
        }

    @Test
    fun `chat messages that land after their trade fill its chat`() =
        runTest {
            selectedUserProfile.value = createMockUserProfile("me")
            facade.activate()
            val trade = openTrade(TRADE_ID)
            openTradeItems.value = listOf(trade)
            advanceUntilIdle()

            tradeChatsObserver.setEvent(tradeChatSnapshot(chatMessage(TRADE_ID)))
            advanceUntilIdle()

            verify { trade.bisqEasyOpenTradeChannelModel.setAllChatMessages(match { it.size == 1 }) }
            facade.deactivate()
        }

    private fun openTrade(
        tradeId: String,
        myUserProfile: UserProfileVO = createMockUserProfile("me"),
    ): TradeItemPresentationModel =
        mockk(relaxed = true) {
            every { this@mockk.tradeId } returns tradeId
            every { this@mockk.myUserProfile } returns myUserProfile
        }

    private fun chatMessage(
        tradeId: String,
        sender: UserProfileVO = createMockUserProfile("peer"),
    ): BisqEasyOpenTradeMessageDto =
        BisqEasyOpenTradeMessageDto(
            tradeId = tradeId,
            messageId = "msg-$tradeId",
            channelId = "channel-$tradeId",
            senderUserProfile = sender,
            receiverUserProfileId = "me",
            receiverNetworkId = sender.networkId,
            text = "Payment sent",
            citation = null,
            date = 1234567890000L,
            mediator = null,
            chatMessageType = ChatMessageTypeEnum.TEXT,
            bisqEasyOffer = null,
            chatMessageReactions = emptySet(),
            citationAuthorUserProfile = null,
        )

    private fun tradeChatSnapshot(vararg messages: BisqEasyOpenTradeMessageDto) =
        WebSocketEvent(
            topic = Topic.TRADE_CHAT_MESSAGES,
            subscriberId = "trade-chat-test",
            deferredPayload = json.encodeToString(messages.toList()),
            modificationType = ModificationType.REPLACE,
            sequenceNumber = 1,
        )

    @Test
    fun `chat reactions subscription waits for the first open trade and then collects events`() =
        runTest {
            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeChatReactions() }

            openTradeItems.value = listOf(mockk(relaxed = true))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeChatReactions() }

            chatReactionsObserver.setEvent(
                WebSocketEvent(
                    topic = Topic.CHAT_REACTIONS,
                    subscriberId = "chat-reactions-test",
                    deferredPayload = "[]",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            facade.deactivate()
        }
}
