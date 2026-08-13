package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.TradeReadStateMap
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferCoordinatorTest {
    // --- Fakes (Android/JVM-friendly) ---
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                },
            )
        }
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private class FakeTradesServiceFacade : TradesServiceFacade {
        override val selectedTrade: StateFlow<TradeItemPresentationModel?> = MutableStateFlow(null)
        override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList())
        override val closedTradesChangeTick: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun getClosedTradesPaginated(
            params: PaginationParams,
            search: String?,
            sortBy: TradeSort?,
            outcomeFilter: TradeOutcomeFilter,
            roleFilter: TradeRoleFilter,
        ): Result<PaginatedResponse<ClosedTradeListItem>> = Result.success(PaginatedResponse(emptyList(), params.page, params.pageSize, 0L, 0))

        override suspend fun takeOffer(
            bisqEasyOffer: BisqEasyOfferVO,
            takersBaseSideAmount: MonetaryVO,
            takersQuoteSideAmount: MonetaryVO,
            bitcoinPaymentMethod: String,
            fiatPaymentMethod: String,
            takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
            takeOfferErrorMessage: MutableStateFlow<String?>,
        ): Result<String> = Result.success("trade-1")

        override fun selectOpenTrade(tradeId: String) {}

        override suspend fun rejectTrade(reason: AnalyticsEvent.Trade.InterruptReason): Result<Unit> = Result.success(Unit)

        override suspend fun cancelTrade(reason: AnalyticsEvent.Trade.InterruptReason): Result<Unit> = Result.success(Unit)

        override suspend fun closeTrade(): Result<Unit> = Result.success(Unit)

        override suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit> = Result.success(Unit)

        override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit> = Result.success(Unit)

        override suspend fun sellerConfirmFiatReceipt(): Result<Unit> = Result.success(Unit)

        override suspend fun buyerConfirmFiatSent(): Result<Unit> = Result.success(Unit)

        override suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit> = Result.success(Unit)

        override suspend fun btcConfirmed(): Result<Unit> = Result.success(Unit)

        override suspend fun exportTradeDate(): Result<Unit> = Result.success(Unit)

        override fun resetSelectedTradeToNull() {}
    }

    @Test
    fun selectOfferToTake_fixedAmountSpec_noAmountRange() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade())

        // Act: Select offer with fixed amount
        val fixedAmountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = fixedAmountSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: No amount range, amounts are set from the fixed spec
        assertFalse(presenter.takeOfferModel.hasAmountRange)
        assertFalse(presenter.showAmountScreen())
        assertEquals(500_000L, presenter.takeOfferModel.quoteAmount.value)
        assertTrue(presenter.takeOfferModel.baseAmount.value > 0)
        assertEquals(1, presenter.totalSteps) // No amount screen added
    }

    @Test
    fun selectOfferToTake_wideRange_hasAmountRange() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade())

        // Act: Select offer with wide range (100_000 to 5_000_000)
        // Trade limits: MIN $6 = 60_000, MAX $600 = 6_000_000
        // Effective range: 100_000 to 5_000_000
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Has amount range because (5_000_000 - 100_000) >= 10_000
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.showAmountScreen())
        assertEquals(2, presenter.totalSteps) // Amount screen added
    }

    @Test
    fun selectOfferToTake_collapsedRange_noAmountRange_setsFixedAmount() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade())

        // Act: Select offer where range collapses after clamping
        // Offer range: 1_070_000 to 1_075_000 (difference = 5_000, which is < 10_000 slider step)
        // After clamping with trade limits (60_000 to 6_000_000), effective range is still 1_070_000 to 1_075_000
        // Since (1_075_000 - 1_070_000) = 5_000 < 10_000, range collapses
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 1_070_000L, maxAmount = 1_075_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Range collapsed, amounts set to midpoint
        assertFalse(presenter.takeOfferModel.hasAmountRange)
        assertFalse(presenter.showAmountScreen())
        // Midpoint: (1_070_000 + 1_075_000) / 2 = 1_072_500
        assertEquals(1_072_500L, presenter.takeOfferModel.quoteAmount.value)
        assertTrue(presenter.takeOfferModel.baseAmount.value > 0)
        assertEquals(1, presenter.totalSteps)
    }

    @Test
    fun selectOfferToTake_missingMarketPrice_fallsBackToShowAmountScreen() {
        // Arrange: Empty prices map (no market price data)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, emptyMap())

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade())

        // Act: Select offer with range spec
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Falls back to showing amount screen when trade limits are 0
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.showAmountScreen())
        assertEquals(2, presenter.totalSteps)
    }

    @Test
    fun selectOfferToTake_invertedRange_fallsBackToShowAmountScreen() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade())

        // Act: Select offer where min > max trade limit
        // Trade limits: MIN $6 = 60_000, MAX $600 = 6_000_000
        // Offer min = 7_000_000 > trade limit max = 6_000_000
        // This creates an inverted range: effectiveMin > effectiveMax
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 7_000_000L, maxAmount = 10_000_000L)
        val dto = OfferTestFactory.makeOfferDto(amountSpec = rangeSpec)
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Falls back to showing amount screen for inverted range
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.showAmountScreen())
        assertEquals(2, presenter.totalSteps)
    }

    @Test
    fun selectOfferToTake_multiplePaymentMethods_incrementsTotalSteps() {
        // Arrange: USD market at $100,000/BTC
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = SettingsRepositoryMock()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade, FakeConfigServiceFacade())

        // Act: Select offer with wide range and 2 quote payment methods
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto =
            OfferTestFactory.makeOfferDto(
                amountSpec = rangeSpec,
                paymentMethods = listOf("SEPA", "Wise"),
                btcMethods = listOf("BTC"),
            )
        val model = OfferItemPresentationModel(dto)
        presenter.selectOfferToTake(model)

        // Assert: Total steps = 1 (base) + 1 (amount) + 1 (payment methods) = 3
        assertTrue(presenter.takeOfferModel.hasAmountRange)
        assertTrue(presenter.takeOfferModel.hasMultipleQuoteSidePaymentMethods)
        assertTrue(presenter.showAmountScreen())
        assertTrue(presenter.showPaymentMethodsScreen())
        assertEquals(3, presenter.totalSteps)
    }
}
