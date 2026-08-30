package network.bisq.mobile.node.common.domain.service.chat.trade

import bisq.chat.ChatMessageType
import bisq.chat.ChatService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.network.SendMessageResult
import bisq.user.UserService
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.I2pKeyPairVO
import network.bisq.mobile.data.replicated.security.keys.KeyBundleVO
import network.bisq.mobile.data.replicated.security.keys.KeyPairVO
import network.bisq.mobile.data.replicated.security.keys.PrivateKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.keys.TorKeyPairVO
import network.bisq.mobile.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.node.common.domain.mapping.chat.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel as Bisq2BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage as Bisq2BisqEasyOpenTradeMessage
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction as Bisq2BisqEasyOpenTradeMessageReaction

@OptIn(ExperimentalCoroutinesApi::class)
class NodeTradeChatMessagesServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private val myUserProfile = createMockUserProfile("me")
    private val peerUserProfile = createMockUserProfile("peer")

    private val channelService: BisqEasyOpenTradeChannelService = mockk(relaxed = true)
    private val userIdentityService: UserIdentityService = mockk(relaxed = true)
    private val userProfileService: UserProfileService = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade = mockk(relaxed = true)

    private lateinit var channelModel: BisqEasyOpenTradeChannel
    private lateinit var openTradeItemsFlow: MutableStateFlow<List<TradeItemPresentationModel>>
    private lateinit var facade: NodeTradeChatMessagesServiceFacade
    private lateinit var messageObserver: CollectionObserver<Bisq2BisqEasyOpenTradeMessage>

    override fun onSetup() {
        // Production uses Dispatchers.Default; route it onto the shared test dispatcher.
        // Base already set Main — mockkStatic then re-stub Default without breaking Main.
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Default } returns testDispatcher

        channelModel = createChannel()
        openTradeItemsFlow =
            MutableStateFlow(
                listOf(
                    mockk<TradeItemPresentationModel> {
                        every { tradeId } returns TRADE_ID
                        every { bisqEasyOpenTradeChannelModel } returns channelModel
                    },
                ),
            )
        every { tradesServiceFacade.openTradeItems } returns openTradeItemsFlow

        val userIdentity = mockk<UserIdentity>(relaxed = true)
        every { userIdentityService.selectedUserIdentity } returns userIdentity

        // Extension functions compile to statics on the file class, so mockkStatic on that class
        // is the equivalent of the mockkObject this used before the mappings moved out of Mappings.
        mockkStatic(BISQ_EASY_OPEN_TRADE_MESSAGE_MAPPING_CLASS)
        every {
            any<Bisq2BisqEasyOpenTradeMessage>().toDomain(any(), any())
        } answers {
            // args[0] is the extension receiver, i.e. the message.
            val message = args[0] as Bisq2BisqEasyOpenTradeMessage
            modelForMessageId(message.id)
        }

        val chatService = mockk<ChatService>()
        every { chatService.bisqEasyOpenTradeChannelService } returns channelService

        val userService = mockk<UserService>()
        every { userService.userIdentityService } returns userIdentityService
        every { userService.userProfileService } returns userProfileService
        every { userProfileService.findUserProfile(any()) } returns Optional.empty()

        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.chatService } returns chatService
        every { applicationService.userService } returns userService

        val provider = AndroidApplicationService.Provider()
        provider.applicationService = applicationService

        facade =
            NodeTradeChatMessagesServiceFacade(
                applicationService = provider,
                tradesServiceFacade = tradesServiceFacade,
                messageDeliveryServiceFacade = messageDeliveryServiceFacade,
            )

        bindChannelObserver(mockChannel())
    }

    override fun onTearDown() {
        try {
            unmockkStatic(BISQ_EASY_OPEN_TRADE_MESSAGE_MAPPING_CLASS)
            unmockkStatic(Dispatchers::class)
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `onAllAdded loads visible messages and skips TAKE_BISQ_EASY_OFFER`() =
        runTest {
            val takeOffer = createBisq2Message("take-1", ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER)
            val protocolLog = createBisq2Message("log-1", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE)
            val text = createBisq2Message("text-1", ChatMessageTypeEnum.TEXT)

            messageObserver.onAllAdded(listOf(takeOffer, protocolLog, text))

            assertEquals(2, channelModel.chatMessages.value.size)
            assertTrue(channelModel.chatMessages.value.any { it.id == "log-1" })
            assertTrue(channelModel.chatMessages.value.any { it.id == "text-1" })
            verify(exactly = 0) { channelService.persist() }

            advanceTimeBy(NodeTradeChatMessagesServiceFacade.PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS + 1_000)
            advanceUntilIdle()
            verify(exactly = 0) { channelService.persist() }
        }

    @Test
    fun `onAdded skips TAKE_BISQ_EASY_OFFER`() =
        runTest {
            val takeOffer = createBisq2Message("take-1", ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER)

            messageObserver.onAdded(takeOffer)

            assertTrue(channelModel.chatMessages.value.isEmpty())
            verify(exactly = 0) { channelService.persist() }

            advanceTimeBy(NodeTradeChatMessagesServiceFacade.PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS + 1_000)
            advanceUntilIdle()
            verify(exactly = 0) { channelService.persist() }
        }

    @Test
    fun `onAdded ignores messages when trade is not open`() =
        runTest {
            openTradeItemsFlow.value = emptyList()
            val text = createBisq2Message("text-1", ChatMessageTypeEnum.TEXT)

            messageObserver.onAdded(text)

            assertTrue(channelModel.chatMessages.value.isEmpty())
            verify(exactly = 0) { channelService.persist() }
        }

    @Test
    fun `onAdded schedules persist for live PROTOCOL_LOG_MESSAGE after delay`() =
        runTest {
            val protocolLog = createBisq2Message("log-1", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE)

            messageObserver.onAdded(protocolLog)

            assertEquals(1, channelModel.chatMessages.value.size)
            verify(exactly = 0) { channelService.persist() }

            advanceTimeBy(NodeTradeChatMessagesServiceFacade.PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS - 1)
            verify(exactly = 0) { channelService.persist() }

            advanceTimeBy(1)
            advanceUntilIdle()
            verify(exactly = 1) { channelService.persist() }
        }

    @Test
    fun `onAdded schedules persist for each live PROTOCOL_LOG_MESSAGE`() =
        runTest {
            messageObserver.onAdded(createBisq2Message("log-1", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE))
            messageObserver.onAdded(createBisq2Message("log-2", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE))

            advanceTimeBy(NodeTradeChatMessagesServiceFacade.PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS)
            advanceUntilIdle()
            verify(exactly = 2) { channelService.persist() }
        }

    /**
     * `TradeChatPresenter` only reads `onSuccess`, so a message that never left has to come back as a
     * failure — an unconditional `Result.success` would clear the quoted message and show nothing.
     */
    @Test
    fun `sendChatMessage fails when no trade is selected`() =
        runTest {
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)

            val result = facade.sendChatMessage("hello", citation = null)

            assertTrue(result.isFailure)
            verify(exactly = 0) { channelService.sendTextMessage(any(), any(), any()) }
        }

    @Test
    fun `sendChatMessage fails when the selected trade has no channel`() =
        runTest {
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(openTradeItemsFlow.value.single())
            every { channelService.findChannel(CHANNEL_ID) } returns Optional.empty()

            val result = facade.sendChatMessage("hello", citation = null)

            assertTrue(result.isFailure)
            verify(exactly = 0) { channelService.sendTextMessage(any(), any(), any()) }
        }

    /**
     * The two cases above never reach the node. This one does, and it is the one that matters: Bisq 2
     * dispatches asynchronously, so a send that fails on the wire fails inside the returned future and
     * nowhere else. Awaiting it is the only thing separating a real failure from a silent one, and
     * without this test removing that `await` leaves the suite green.
     */
    @Test
    fun `sendChatMessage fails when the dispatch itself fails`() =
        runTest {
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(openTradeItemsFlow.value.single())
            every { channelService.findChannel(CHANNEL_ID) } returns Optional.of(mockk<Bisq2BisqEasyOpenTradeChannel>(relaxed = true))
            every { channelService.sendTextMessage(any(), any(), any()) } returns
                CompletableFuture.failedFuture(IllegalStateException("no peer reachable"))

            val result = facade.sendChatMessage("hello", citation = null)

            assertTrue(result.isFailure)
        }

    @Test
    fun `sendChatMessage succeeds when the dispatch completes`() =
        runTest {
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(openTradeItemsFlow.value.single())
            every { channelService.findChannel(CHANNEL_ID) } returns Optional.of(mockk<Bisq2BisqEasyOpenTradeChannel>(relaxed = true))
            every { channelService.sendTextMessage(any(), any(), any()) } returns
                CompletableFuture.completedFuture(mockk<SendMessageResult>(relaxed = true))

            val result = facade.sendChatMessage("hello", citation = null)

            assertTrue(result.isSuccess)
        }

    private fun bindChannelObserver(channel: Bisq2BisqEasyOpenTradeChannel) {
        val observerSlot = slot<CollectionObserver<Bisq2BisqEasyOpenTradeMessage>>()
        every { channel.chatMessages.addObserver(capture(observerSlot)) } returns mockk<Pin>(relaxed = true)

        val method =
            NodeTradeChatMessagesServiceFacade::class.java.getDeclaredMethod(
                "handleChannelAdded",
                Bisq2BisqEasyOpenTradeChannel::class.java,
            )
        method.isAccessible = true
        method.invoke(facade, channel)

        messageObserver = observerSlot.captured
    }

    private fun mockChannel(): Bisq2BisqEasyOpenTradeChannel {
        val chatMessages = mockk<ObservableSet<Bisq2BisqEasyOpenTradeMessage>>()
        return mockk {
            every { tradeId } returns TRADE_ID
            every { this@mockk.chatMessages } returns chatMessages
        }
    }

    private fun createChannel(): BisqEasyOpenTradeChannel {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val offer =
            BisqEasyOfferVO(
                id = "offer-1",
                date = 0L,
                makerNetworkId = myUserProfile.networkId,
                direction = DirectionEnum.BUY,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(100_00),
                priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.run { fromPrice(100_00L, market) }),
                protocolTypes = listOf(TradeProtocolTypeEnum.BISQ_EASY),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return BisqEasyOpenTradeChannel(
            id = CHANNEL_ID,
            tradeId = TRADE_ID,
            bisqEasyOffer = offer,
            myUserIdentity = createUserIdentity(myUserProfile),
            traders = setOf(peerUserProfile),
            mediator = null,
        )
    }

    private fun createUserIdentity(userProfile: UserProfileVO): UserIdentityVO =
        UserIdentityVO(
            identity =
                IdentityVO(
                    tag = "identity-1",
                    networkId = userProfile.networkId,
                    keyBundle =
                        KeyBundleVO(
                            keyId = "key-1",
                            keyPair =
                                KeyPairVO(
                                    publicKey = PublicKeyVO("public-key"),
                                    privateKey = PrivateKeyVO("private-key"),
                                ),
                            torKeyPair =
                                TorKeyPairVO(
                                    privateKeyEncoded = "tor-private",
                                    publicKeyEncoded = "tor-public",
                                    onionAddress = "address.onion",
                                ),
                            i2pKeyPair =
                                I2pKeyPairVO(
                                    identityBytes = "identity-bytes",
                                    destinationBytes = "destination-bytes",
                                ),
                        ),
                ),
            userProfile = userProfile,
        )

    private fun modelForMessageId(messageId: String): BisqEasyOpenTradeMessage =
        createMockBisqEasyOpenTradeMessage(
            id = messageId,
            text = "hello",
            date = 1L,
            senderUserProfile = peerUserProfile,
            myUserProfile = myUserProfile,
            tradeId = TRADE_ID,
        )

    private fun createBisq2Message(
        messageId: String,
        type: ChatMessageTypeEnum,
    ): Bisq2BisqEasyOpenTradeMessage {
        val reactions = mockk<ObservableSet<Bisq2BisqEasyOpenTradeMessageReaction>>(relaxed = true)

        return mockk {
            every { id } returns messageId
            every { chatMessageType } returns type.toBisq2Model()
            every { citation } returns Optional.empty()
            every { chatMessageReactions } returns reactions
        }
    }

    private fun ChatMessageTypeEnum.toBisq2Model(): ChatMessageType =
        when (this) {
            ChatMessageTypeEnum.TEXT -> ChatMessageType.TEXT
            ChatMessageTypeEnum.TAKE_BISQ_EASY_OFFER -> ChatMessageType.TAKE_BISQ_EASY_OFFER
            ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE -> ChatMessageType.PROTOCOL_LOG_MESSAGE
            ChatMessageTypeEnum.LEAVE -> ChatMessageType.LEAVE
            ChatMessageTypeEnum.CHAT_RULES_WARNING -> ChatMessageType.CHAT_RULES_WARNING
            ChatMessageTypeEnum.EXPIRED_MESSAGES_INDICATOR -> ChatMessageType.EXPIRED_MESSAGES_INDICATOR
        }

    companion object {
        private const val TRADE_ID = "trade-1"
        private const val CHANNEL_ID = "channel-1"

        /** JVM file class holding the `Bisq2BisqEasyOpenTradeMessage.toDomain` extension. */
        private const val BISQ_EASY_OPEN_TRADE_MESSAGE_MAPPING_CLASS =
            "network.bisq.mobile.node.common.domain.mapping.chat.BisqEasyOpenTradeMessageMappingKt"
    }
}
