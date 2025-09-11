package network.bisq.mobile.presentation

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

@OptIn(ExperimentalCoroutinesApi::class)
class MainPresenterUnreadBadgeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `trades in final states are excluded from unread badge map`() = runTest {
        // Mock top-level android-specific function called from MainPresenter.init
        mockkStatic("network.bisq.mobile.presentation.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        // Dependencies
        val connectivity = mockk<ConnectivityService>(relaxed = true)
        val notifications = mockk<OpenTradesNotificationService>(relaxed = true)
        val settings = mockk<SettingsServiceFacade>()
        every { settings.languageCode } returns MutableStateFlow("en")
        every { settings.useAnimations } returns MutableStateFlow(false)
        val tradesFacade = mockk<TradesServiceFacade>()
        val readRepo = mockk<TradeReadStateRepository>()
        val urlLauncher = mockk<UrlLauncher>(relaxed = true)

        // Trade item with message and state flows
        val chatMessagesFlow = MutableStateFlow<Set<BisqEasyOpenTradeMessageModel>>(emptySet())
        val tradeStateFlow = MutableStateFlow(BisqEasyTradeStateEnum.INIT)

        val channelModel = mockk<BisqEasyOpenTradeChannelModel>()
        every { channelModel.chatMessages } returns chatMessagesFlow
        val tradeModel = mockk<BisqEasyTradeModel>()
        every { tradeModel.tradeState } returns tradeStateFlow

        val tradeItem = mockk<TradeItemPresentationModel>()
        every { tradeItem.tradeId } returns "t1"
        every { tradeItem.bisqEasyOpenTradeChannelModel } returns channelModel
        every { tradeItem.bisqEasyTradeModel } returns tradeModel

        val openTradesFlow = MutableStateFlow(listOf(tradeItem))
        every { tradesFacade.openTradeItems } returns openTradesFlow
        every { tradesFacade.selectedTrade } returns MutableStateFlow<TradeItemPresentationModel?>(null)

        val readMapFlow = MutableStateFlow(TradeReadStateMap(mapOf("t1" to 0)))
        every { readRepo.data } returns readMapFlow

        val presenter = MainPresenter(
            connectivityService = connectivity,
            openTradesNotificationService = notifications,
            settingsService = settings,
            tradesServiceFacade = tradesFacade,
            tradeReadStateRepository = readRepo,
            urlLauncher = urlLauncher
        )

        // Initially no messages => no unread
        var unread = presenter.tradesWithUnreadMessages.first()
        assertTrue(unread.isEmpty())

        // Add some messages while readCount is 0 => unread present
        val m1 = mockk<BisqEasyOpenTradeMessageModel>(relaxed = true)
        val m2 = mockk<BisqEasyOpenTradeMessageModel>(relaxed = true)
        val m3 = mockk<BisqEasyOpenTradeMessageModel>(relaxed = true)
        chatMessagesFlow.value = setOf(m1, m2, m3)

        // Allow flow to propagate
        delay(10)
        unread = presenter.tradesWithUnreadMessages.first()
        assertTrue(unread.containsKey("t1"))

        // Transition trade to final state => it should be excluded from unread
        tradeStateFlow.value = BisqEasyTradeStateEnum.CANCELLED

        // Allow flow to propagate
        delay(10)
        unread = presenter.tradesWithUnreadMessages.first()
        assertFalse(unread.containsKey("t1"))
    }
}

