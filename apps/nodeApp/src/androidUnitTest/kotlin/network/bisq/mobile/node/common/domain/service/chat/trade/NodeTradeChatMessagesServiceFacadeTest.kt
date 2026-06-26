package network.bisq.mobile.node.common.domain.service.chat.trade

import bisq.chat.ChatMessageType
import bisq.chat.ChatService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.user.UserService
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
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
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NodeTradeChatMessagesServiceFacadeTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var channelService: BisqEasyOpenTradeChannelService
    private lateinit var userIdentityService: UserIdentityService
    private lateinit var userProfileService: UserProfileService
    private lateinit var tradesServiceFacade: TradesServiceFacade
    private lateinit var openTradeItemsFlow: MutableStateFlow<List<TradeItemPresentationModel>>
    private lateinit var channelModel: BisqEasyOpenTradeChannelModel
    private lateinit var facade: NodeTradeChatMessagesServiceFacade
    private lateinit var messageObserver: CollectionObserver<BisqEasyOpenTradeMessage>

    private val myUserProfile = createMockUserProfile("me")
    private val peerUserProfile = createMockUserProfile("peer")

    @Before
    fun setUp() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Default } returns testDispatcher
        Dispatchers.setMain(testDispatcher)

        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                },
            )
        }

        channelService = mockk(relaxed = true)
        userIdentityService = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        tradesServiceFacade = mockk(relaxed = true)

        channelModel = BisqEasyOpenTradeChannelModel(createChannelDto())
        val tradeItem =
            mockk<TradeItemPresentationModel> {
                every { tradeId } returns TRADE_ID
                every { bisqEasyOpenTradeChannelModel } returns channelModel
            }
        openTradeItemsFlow = MutableStateFlow(listOf(tradeItem))
        every { tradesServiceFacade.openTradeItems } returns openTradeItemsFlow

        val userIdentity = mockk<UserIdentity>(relaxed = true)
        every { userIdentityService.selectedUserIdentity } returns userIdentity

        mockkObject(Mappings.BisqEasyOpenTradeMessageModelMapping)
        every {
            Mappings.BisqEasyOpenTradeMessageModelMapping.fromBisq2Model(any(), any(), any())
        } answers {
            val message = args[0] as BisqEasyOpenTradeMessage
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
                messageDeliveryServiceFacade = mockk<MessageDeliveryServiceFacade>(relaxed = true),
            )

        bindChannelObserver(mockChannel())
    }

    @After
    fun tearDown() {
        unmockkObject(Mappings.BisqEasyOpenTradeMessageModelMapping)
        unmockkStatic(Dispatchers::class)
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `onAllAdded loads visible messages and skips TAKE_BISQ_EASY_OFFER`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            openTradeItemsFlow.value = emptyList()
            val text = createBisq2Message("text-1", ChatMessageTypeEnum.TEXT)

            messageObserver.onAdded(text)

            assertTrue(channelModel.chatMessages.value.isEmpty())
            verify(exactly = 0) { channelService.persist() }
        }

    @Test
    fun `onAdded schedules persist for live PROTOCOL_LOG_MESSAGE after delay`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            messageObserver.onAdded(createBisq2Message("log-1", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE))
            messageObserver.onAdded(createBisq2Message("log-2", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE))

            advanceTimeBy(NodeTradeChatMessagesServiceFacade.PERSIST_DELAY_AFTER_PROTOCOL_LOG_MS)
            advanceUntilIdle()
            verify(exactly = 2) { channelService.persist() }
        }

    private fun bindChannelObserver(channel: BisqEasyOpenTradeChannel) {
        val observerSlot = slot<CollectionObserver<BisqEasyOpenTradeMessage>>()
        every { channel.chatMessages.addObserver(capture(observerSlot)) } returns mockk<Pin>(relaxed = true)

        val method =
            NodeTradeChatMessagesServiceFacade::class.java.getDeclaredMethod(
                "handleChannelAdded",
                BisqEasyOpenTradeChannel::class.java,
            )
        method.isAccessible = true
        method.invoke(facade, channel)

        messageObserver = observerSlot.captured
    }

    private fun mockChannel(): BisqEasyOpenTradeChannel {
        val chatMessages = mockk<ObservableSet<BisqEasyOpenTradeMessage>>()
        return mockk {
            every { tradeId } returns TRADE_ID
            every { this@mockk.chatMessages } returns chatMessages
        }
    }

    private fun createChannelDto(): BisqEasyOpenTradeChannelDto {
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
        return BisqEasyOpenTradeChannelDto(
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

    private fun modelForMessageId(messageId: String): BisqEasyOpenTradeMessageModel =
        BisqEasyOpenTradeMessageModel(
            network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto(
                tradeId = TRADE_ID,
                messageId = messageId,
                channelId = CHANNEL_ID,
                senderUserProfile = peerUserProfile,
                receiverUserProfileId = myUserProfile.id,
                receiverNetworkId = myUserProfile.networkId,
                text = "hello",
                citation = null,
                date = 1L,
                mediator = null,
                chatMessageType = ChatMessageTypeEnum.TEXT,
                bisqEasyOffer = null,
                chatMessageReactions = emptySet(),
                citationAuthorUserProfile = null,
            ),
            myUserProfile,
            emptyList(),
        )

    private fun createBisq2Message(
        messageId: String,
        type: ChatMessageTypeEnum,
    ): BisqEasyOpenTradeMessage {
        val reactions = mockk<ObservableSet<BisqEasyOpenTradeMessageReaction>>(relaxed = true)

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
    }
}
