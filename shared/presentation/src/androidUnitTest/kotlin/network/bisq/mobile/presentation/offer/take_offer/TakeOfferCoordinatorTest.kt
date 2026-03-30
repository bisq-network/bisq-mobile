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
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.TradeReadStateMap
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
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

    private class FakeSettingsRepository : SettingsRepository {
        private val _data = MutableStateFlow(Settings())
        override val data: StateFlow<Settings> = _data

        override suspend fun setFirstLaunch(value: Boolean) {}

        override suspend fun setShowChatRulesWarnBox(value: Boolean) {}

        override suspend fun setSelectedMarketCode(value: String) {}

        override suspend fun setNotificationPermissionState(value: PermissionState) {}

        override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {}

        override suspend fun update(transform: suspend (t: Settings) -> Settings) {
            _data.value = transform(_data.value)
        }

        override suspend fun clear() {
            _data.value = Settings()
        }

        override suspend fun setMarketSortBy(value: MarketSortBy) {}

        override suspend fun setMarketFilter(value: MarketFilter) {}

        override suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean) {}

        override suspend fun setPermitOpeningBrowser(value: Boolean) {}
    }

    private class FakeMarketPriceServiceFacade(
        settingsRepository: SettingsRepository,
        private val prices: Map<MarketVO, MarketPriceItem>,
    ) : MarketPriceServiceFacade(settingsRepository) {
        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? =
            prices.entries
                .firstOrNull { (k, _) ->
                    k.baseCurrencyCode == marketVO.baseCurrencyCode && k.quoteCurrencyCode == marketVO.quoteCurrencyCode
                }?.value

        override fun findUSDMarketPriceItem(): MarketPriceItem? = findMarketPriceItem(MarketVO("BTC", "USD"))

        override fun refreshSelectedFormattedMarketPrice() {}

        override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
    }

    private class FakeTradesServiceFacade : TradesServiceFacade {
        override val selectedTrade: StateFlow<TradeItemPresentationModel?> = MutableStateFlow(null)
        override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList())

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

        override suspend fun rejectTrade(): Result<Unit> = Result.success(Unit)

        override suspend fun cancelTrade(): Result<Unit> = Result.success(Unit)

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

    private fun makeOfferDto(
        amountSpec: AmountSpecVO = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L),
        paymentMethods: List<String> = listOf("SEPA"),
        btcMethods: List<String> = listOf("BTC"),
    ): OfferItemPresentationDto {
        val market = MarketVOFactory.USD
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_000_00L, market) })
        val makerNetworkId = NetworkIdVO(AddressByTransportTypeMapVO(mapOf()), PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"))
        val quoteSideSpecs = paymentMethods.map { FiatPaymentMethodSpecVO(it, null) }
        val baseSideSpecs = btcMethods.map { BitcoinPaymentMethodSpecVO(it, null) }
        val offer =
            BisqEasyOfferVO(
                id = "offer-1",
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.BUY,
                market = market,
                amountSpec = amountSpec,
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = baseSideSpecs,
                quoteSidePaymentMethodSpecs = quoteSideSpecs,
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        val user = createMockUserProfile("Alice")
        val reputation = ReputationScoreVO(0, 0.0, 0)
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = user,
            formattedDate = "",
            formattedQuoteAmount = "",
            formattedBaseAmount = "",
            formattedPrice = "",
            formattedPriceSpec = "",
            quoteSidePaymentMethods = paymentMethods,
            baseSidePaymentMethods = btcMethods,
            reputationScore = reputation,
        )
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
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade)

        // Act: Select offer with fixed amount
        val fixedAmountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L)
        val dto = makeOfferDto(amountSpec = fixedAmountSpec)
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
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade)

        // Act: Select offer with wide range (100_000 to 5_000_000)
        // Trade limits: MIN $6 = 60_000, MAX $600 = 6_000_000
        // Effective range: 100_000 to 5_000_000
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto = makeOfferDto(amountSpec = rangeSpec)
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
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade)

        // Act: Select offer where range collapses after clamping
        // Offer range: 1_070_000 to 1_075_000 (difference = 5_000, which is < 10_000 slider step)
        // After clamping with trade limits (60_000 to 6_000_000), effective range is still 1_070_000 to 1_075_000
        // Since (1_075_000 - 1_070_000) = 5_000 < 10_000, range collapses
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 1_070_000L, maxAmount = 1_075_000L)
        val dto = makeOfferDto(amountSpec = rangeSpec)
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
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, emptyMap())

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade)

        // Act: Select offer with range spec
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto = makeOfferDto(amountSpec = rangeSpec)
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
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade)

        // Act: Select offer where min > max trade limit
        // Trade limits: MIN $6 = 60_000, MAX $600 = 6_000_000
        // Offer min = 7_000_000 > trade limit max = 6_000_000
        // This creates an inverted range: effectiveMin > effectiveMax
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 7_000_000L, maxAmount = 10_000_000L)
        val dto = makeOfferDto(amountSpec = rangeSpec)
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
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val tradesServiceFacade = FakeTradesServiceFacade()
        val presenter = TakeOfferCoordinator(marketPriceServiceFacade, tradesServiceFacade)

        // Act: Select offer with wide range and 2 quote payment methods
        val rangeSpec = QuoteSideRangeAmountSpecVO(minAmount = 100_000L, maxAmount = 5_000_000L)
        val dto =
            makeOfferDto(
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
