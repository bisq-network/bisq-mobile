package network.bisq.mobile.client.common.domain.service.chat.trade

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test

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
    private val json = Json { ignoreUnknownKeys = true }
    private val openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    private lateinit var facade: ClientTradeChatMessagesServiceFacade

    override fun onSetup() {
        every { tradesServiceFacade.openTradeItems } returns openTradeItems
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
        facade =
            ClientTradeChatMessagesServiceFacade(
                tradesServiceFacade,
                userProfileServiceFacade,
                apiGateway,
                json,
                globalUiManager,
            )
    }

    @Test
    fun `trade chats subscription waits for the first open trade and then collects events`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeTradeChats() } returns observer

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeTradeChats() }

            openTradeItems.value = listOf(mockk(relaxed = true))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeTradeChats() }

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_CHAT_MESSAGES,
                    subscriberId = "trade-chat-test",
                    deferredPayload = "[]",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            facade.deactivate()
        }

    @Test
    fun `chat reactions subscription waits for the first open trade and then collects events`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeChatReactions() } returns observer

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeChatReactions() }

            openTradeItems.value = listOf(mockk(relaxed = true))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeChatReactions() }

            observer.setEvent(
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
