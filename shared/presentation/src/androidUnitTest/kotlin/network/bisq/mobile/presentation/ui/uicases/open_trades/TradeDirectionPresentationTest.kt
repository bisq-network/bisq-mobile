package network.bisq.mobile.presentation.ui.uicases.open_trades

import kotlinx.coroutines.ExperimentalCoroutinesApi

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeDetailsHeaderPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TradeDirectionPresentationTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    // Minimal Koin module for BasePresenter jobsManager injection
    private val testKoinModule = module {
        single { CoroutineExceptionHandlerSetup() }
        factory<CoroutineJobsManager> {
            DefaultCoroutineJobsManager().apply {
                get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
            }
        }
    }

    @BeforeTest
    fun setup() {
        // Coroutine Main dispatcher for presenters
        Dispatchers.setMain(testDispatcher)
        // Minimal Koin context for CoroutineJobsManager used by BasePresenter
        startKoin { modules(testKoinModule) }
        // Ensure i18n bundles are loaded
        I18nSupport.initialize("en")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun seller_seesSellingTo_and_amountsMappedBaseThenQuote() = runTest {
        // Main presenter only used for languageCode
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        every { mainPresenter.languageCode } returns MutableStateFlow("en")

        // Trade item model and nested flows
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns true
        every { tradeModel.tradeState } returns MutableStateFlow(BisqEasyTradeStateEnum.INIT)

        val channelModel = mockk<BisqEasyOpenTradeChannelModel>(relaxed = true)
        every { channelModel.isInMediation } returns MutableStateFlow(false)

        val tradeItem = mockk<network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel>(relaxed = true)
        every { tradeItem.bisqEasyTradeModel } returns tradeModel
        every { tradeItem.bisqEasyOpenTradeChannelModel } returns channelModel
        every { tradeItem.formattedBaseAmount } returns "0.00433161"
        every { tradeItem.baseCurrencyCode } returns "BTC"
        every { tradeItem.formattedQuoteAmount } returns "500.00"
        every { tradeItem.quoteCurrencyCode } returns "USD"

        // Services
        val tradesServiceFacade = mockk<TradesServiceFacade>()
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(tradeItem)

        val mediationServiceFacade = mockk<MediationServiceFacade>(relaxed = true)
        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)

        val presenter = TradeDetailsHeaderPresenter(
            mainPresenter = mainPresenter,
            tradesServiceFacade = tradesServiceFacade,
            mediationServiceFacade = mediationServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
        )

        presenter.onViewAttached()

        assertEquals("offer.sell".i18n().uppercase(), presenter.direction)
        assertEquals("bisqEasy.tradeState.header.send".i18n(), presenter.leftAmountDescription)
        assertEquals("bisqEasy.tradeState.header.receive".i18n(), presenter.rightAmountDescription)
        assertEquals("0.00433161", presenter.leftAmount.value)
        assertEquals("BTC", presenter.leftCode.value)
        assertEquals("500.00", presenter.rightAmount.value)
        assertEquals("USD", presenter.rightCode.value)
    }

    @Test
    fun buyer_seesBuyingFrom_and_amountsMappedQuoteThenBase() = runTest {
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        every { mainPresenter.languageCode } returns MutableStateFlow("en")

        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns false
        every { tradeModel.tradeState } returns MutableStateFlow(BisqEasyTradeStateEnum.INIT)

        val channelModel = mockk<BisqEasyOpenTradeChannelModel>(relaxed = true)
        every { channelModel.isInMediation } returns MutableStateFlow(false)

        val tradeItem = mockk<network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel>(relaxed = true)
        every { tradeItem.bisqEasyTradeModel } returns tradeModel
        every { tradeItem.bisqEasyOpenTradeChannelModel } returns channelModel
        every { tradeItem.formattedBaseAmount } returns "0.00433161"
        every { tradeItem.baseCurrencyCode } returns "BTC"
        every { tradeItem.formattedQuoteAmount } returns "500.00"
        every { tradeItem.quoteCurrencyCode } returns "USD"

        val tradesServiceFacade = mockk<TradesServiceFacade>()
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(tradeItem)

        val mediationServiceFacade = mockk<MediationServiceFacade>(relaxed = true)
        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)

        val presenter = TradeDetailsHeaderPresenter(
            mainPresenter = mainPresenter,
            tradesServiceFacade = tradesServiceFacade,
            mediationServiceFacade = mediationServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
        )

        presenter.onViewAttached()

        assertEquals("offer.buy".i18n().uppercase(), presenter.direction)
        assertEquals("bisqEasy.tradeState.header.pay".i18n(), presenter.leftAmountDescription)
        assertEquals("bisqEasy.tradeState.header.receive".i18n(), presenter.rightAmountDescription)
        assertEquals("500.00", presenter.leftAmount.value)
        assertEquals("USD", presenter.leftCode.value)
        assertEquals("0.00433161", presenter.rightAmount.value)
        assertEquals("BTC", presenter.rightCode.value)
    }
}

