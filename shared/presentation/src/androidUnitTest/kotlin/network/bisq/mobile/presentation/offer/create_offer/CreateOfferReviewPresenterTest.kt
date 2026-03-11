package network.bisq.mobile.presentation.offer.create_offer

import io.mockk.every
import io.mockk.mockk
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
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.review.CreateOfferReviewPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOfferReviewPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
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
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    // --- Fakes (reused from CreateOfferPricePresenterTest) ---
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
    }

    private class FakeMarketPriceServiceFacade(
        settingsRepository: SettingsRepository,
        private val prices: MutableMap<MarketVO, MarketPriceItem>,
    ) : MarketPriceServiceFacade(settingsRepository) {
        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? =
            prices.entries
                .firstOrNull { (k, _) ->
                    k.baseCurrencyCode == marketVO.baseCurrencyCode &&
                        k.quoteCurrencyCode == marketVO.quoteCurrencyCode
                }?.value

        override fun findUSDMarketPriceItem(): MarketPriceItem? = findMarketPriceItem(MarketVO("BTC", "USD"))

        override fun refreshSelectedFormattedMarketPrice() {}

        override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)

        fun updatePrice(
            market: MarketVO,
            item: MarketPriceItem,
        ) {
            prices[market] = item
        }
    }

    private class FakeSettingsServiceFacade : SettingsServiceFacade {
        override suspend fun getSettings() = Result.success(settingsVODemoObj)

        override suspend fun confirmTacAccepted(value: Boolean) = Result.success(Unit)

        override val tradeRulesConfirmed: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun confirmTradeRules(value: Boolean) = Result.success(Unit)

        override val languageCode: StateFlow<String> = MutableStateFlow("en")

        override suspend fun setLanguageCode(value: String) = Result.success(Unit)

        override suspend fun setSupportedLanguageCodes(value: Set<String>) = Result.success(Unit)

        override suspend fun setCloseMyOfferWhenTaken(value: Boolean) = Result.success(Unit)

        override suspend fun setMaxTradePriceDeviation(value: Double) = Result.success(Unit)

        override val useAnimations: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun setUseAnimations(value: Boolean) = Result.success(Unit)

        override val difficultyAdjustmentFactor: StateFlow<Double> = MutableStateFlow(1.0)

        override suspend fun setDifficultyAdjustmentFactor(value: Double) = Result.success(Unit)

        override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean) = Result.success(Unit)

        override suspend fun setNumDaysAfterRedactingTradeData(days: Int) = Result.success(Unit)
    }

    private class FakeTradesServiceFacade : TradesServiceFacade {
        override val selectedTrade: StateFlow<TradeItemPresentationModel?> = MutableStateFlow(null)
        override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> =
            MutableStateFlow(emptyList())

        override suspend fun takeOffer(
            bisqEasyOffer: BisqEasyOfferVO,
            takersBaseSideAmount: MonetaryVO,
            takersQuoteSideAmount: MonetaryVO,
            bitcoinPaymentMethod: String,
            fiatPaymentMethod: String,
            takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
            takeOfferErrorMessage: MutableStateFlow<String?>,
        ) = Result.success("trade-1")

        override fun selectOpenTrade(tradeId: String) {}

        override suspend fun rejectTrade(): Result<Unit> = Result.success(Unit)

        override suspend fun cancelTrade(): Result<Unit> = Result.success(Unit)

        override suspend fun closeTrade(): Result<Unit> = Result.success(Unit)

        override suspend fun sellerSendsPaymentAccount(paymentAccountData: String) = Result.success(Unit)

        override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String) = Result.success(Unit)

        override suspend fun sellerConfirmFiatReceipt(): Result<Unit> = Result.success(Unit)

        override suspend fun buyerConfirmFiatSent(): Result<Unit> = Result.success(Unit)

        override suspend fun sellerConfirmBtcSent(paymentProof: String?) = Result.success(Unit)

        override suspend fun btcConfirmed(): Result<Unit> = Result.success(Unit)

        override suspend fun exportTradeDate(): Result<Unit> = Result.success(Unit)

        override fun resetSelectedTradeToNull() {}
    }

    private class FakeUserProfileServiceFacade : UserProfileServiceFacade {
        override val userProfiles: StateFlow<List<UserProfileVO>> = MutableStateFlow(emptyList())
        override val selectedUserProfile: StateFlow<UserProfileVO?> = MutableStateFlow(null)
        override val ignoredProfileIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override val numUserProfiles: StateFlow<Int> = MutableStateFlow(1)

        override suspend fun hasUserProfile(): Boolean = true

        override suspend fun generateKeyPair(
            imageSize: Int,
            result: (String, String, PlatformImage?) -> Unit,
        ) {}

        override suspend fun createAndPublishNewUserProfile(nickName: String) {}

        override suspend fun updateAndPublishUserProfile(
            profileId: String,
            statement: String?,
            terms: String?,
        ) = Result.success(createMockUserProfile("me"))

        override suspend fun getUserIdentityIds(): List<String> = emptyList()

        override suspend fun findUserProfile(profileId: String) = createMockUserProfile(profileId)

        override suspend fun findUserProfiles(ids: List<String>) = ids.map { createMockUserProfile(it) }

        override suspend fun getUserProfileIcon(
            userProfile: UserProfileVO,
            size: Number,
        ) = createEmptyImage()

        override suspend fun getUserProfileIcon(userProfile: UserProfileVO) = createEmptyImage()

        override suspend fun getUserPublishDate(): Long = 0L

        override suspend fun userActivityDetected() {}

        override suspend fun ignoreUserProfile(profileId: String) {}

        override suspend fun undoIgnoreUserProfile(profileId: String) {}

        override suspend fun isUserIgnored(profileId: String): Boolean = false

        override suspend fun getIgnoredUserProfileIds(): Set<String> = emptySet()

        override suspend fun reportUserProfile(
            accusedUserProfile: UserProfileVO,
            message: String,
        ): Result<Unit> = Result.failure(Exception("unused"))

        override suspend fun getOwnedUserProfiles() = Result.failure<List<UserProfileVO>>(Exception("unused"))

        override suspend fun selectUserProfile(id: String) = Result.failure<UserProfileVO>(Exception("unused"))

        override suspend fun deleteUserProfile(id: String) = Result.failure<UserProfileVO>(Exception("unused"))
    }

    private class FakeNotificationController : NotificationController {
        override suspend fun hasPermission(): Boolean = true

        override fun notify(config: NotificationConfig) {}

        override fun cancel(id: String) {}

        override fun isAppInForeground(): Boolean = true
    }

    private class FakeForegroundServiceController : ForegroundServiceController {
        override fun startService() {}

        override fun stopService() {}

        override fun <T> registerObserver(
            flow: Flow<T>,
            onStateChange: suspend (T) -> Unit,
        ) {}

        override fun unregisterObserver(flow: Flow<*>) {}

        override fun unregisterObservers() {}

        override fun isServiceRunning(): Boolean = false

        override fun dispose() {}
    }

    private class FakeForegroundDetector : ForegroundDetector {
        private val _isForeground = MutableStateFlow(true)
        override val isForeground: StateFlow<Boolean> = _isForeground
    }

    private class FakeUrlLauncher : UrlLauncher {
        override fun openUrl(url: String) {}
    }

    private class FakeTradeReadStateRepository : TradeReadStateRepository {
        override val data: Flow<TradeReadStateMap> = flowOf(TradeReadStateMap())

        override suspend fun setCount(
            tradeId: String,
            count: Int,
        ) {}

        override suspend fun clearId(tradeId: String) {}
    }

    // --- Helper factories ---

    private fun makeMainPresenter(): MainPresenter {
        val tradesServiceFacade = FakeTradesServiceFacade()
        val userProfileServiceFacade = FakeUserProfileServiceFacade()
        val notificationController = FakeNotificationController()
        val foregroundServiceController = FakeForegroundServiceController()
        val foregroundDetector = FakeForegroundDetector()
        val openTradesNotificationService =
            OpenTradesNotificationService(
                notificationController,
                foregroundServiceController,
                tradesServiceFacade,
                userProfileServiceFacade,
                foregroundDetector,
            )
        val settingsService = FakeSettingsServiceFacade()
        val tradeReadStateRepository = FakeTradeReadStateRepository()
        val urlLauncher = FakeUrlLauncher()
        return MainPresenter(
            tradesServiceFacade,
            userProfileServiceFacade,
            openTradesNotificationService,
            settingsService,
            tradeReadStateRepository,
            urlLauncher,
            TestApplicationLifecycleService(),
        )
    }

    private fun makeCreateOfferPresenter(
        mainPresenter: MainPresenter,
        marketPriceServiceFacade: MarketPriceServiceFacade,
    ): CreateOfferPresenter {
        val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
        return CreateOfferPresenter(
            mainPresenter,
            marketPriceServiceFacade,
            offersServiceFacade,
            FakeSettingsServiceFacade(),
        )
    }

    private fun makeMarketPriceItem(
        market: MarketVO,
        faceValue: Double,
    ): MarketPriceItem {
        val priceQuote = with(PriceQuoteVOFactory) { fromPrice(faceValue, market) }
        return MarketPriceItem(market, priceQuote, "$faceValue ${market.quoteCurrencyCode}")
    }

    private fun buildModelWithPercentagePricing(
        market: MarketVO,
        staleMarketPrice: PriceQuoteVO,
        percentage: Double,
    ): CreateOfferPresenter.CreateOfferModel {
        val staleOfferPrice = PriceUtil.fromMarketPriceMarkup(staleMarketPrice, percentage)
        return CreateOfferPresenter.CreateOfferModel().also { m ->
            m.market = market
            m.direction = DirectionEnum.BUY
            m.amountType = CreateOfferPresenter.AmountType.FIXED_AMOUNT
            m.quoteSideFixedAmount = with(FiatVOFactory) { fromFaceValue(5000.0, "USD") }
            m.baseSideFixedAmount = with(CoinVOFactory) { fromFaceValue(0.05, "BTC") }
            m.priceType = CreateOfferPresenter.PriceType.PERCENTAGE
            m.percentagePriceValue = percentage
            m.originalPriceQuote = staleMarketPrice
            m.priceQuote = staleOfferPrice
            m.availableQuoteSidePaymentMethods = listOf("REVOLUT")
            m.selectedQuoteSidePaymentMethods = setOf("REVOLUT")
            m.selectedBaseSidePaymentMethods = setOf("MAIN_CHAIN")
        }
    }

    private fun buildModelWithFixedPricing(
        market: MarketVO,
        staleMarketPrice: PriceQuoteVO,
        fixedPrice: PriceQuoteVO,
        percentage: Double,
    ): CreateOfferPresenter.CreateOfferModel =
        CreateOfferPresenter.CreateOfferModel().also { m ->
            m.market = market
            m.direction = DirectionEnum.BUY
            m.amountType = CreateOfferPresenter.AmountType.FIXED_AMOUNT
            m.quoteSideFixedAmount = with(FiatVOFactory) { fromFaceValue(5000.0, "USD") }
            m.baseSideFixedAmount = with(CoinVOFactory) { fromFaceValue(0.05, "BTC") }
            m.priceType = CreateOfferPresenter.PriceType.FIXED
            m.percentagePriceValue = percentage
            m.originalPriceQuote = staleMarketPrice
            m.priceQuote = fixedPrice
            m.availableQuoteSidePaymentMethods = listOf("REVOLUT")
            m.selectedQuoteSidePaymentMethods = setOf("REVOLUT")
            m.selectedBaseSidePaymentMethods = setOf("MAIN_CHAIN")
        }

    private fun buildModelWithRangeAmountPercentagePricing(
        market: MarketVO,
        staleMarketPrice: PriceQuoteVO,
        percentage: Double,
    ): CreateOfferPresenter.CreateOfferModel {
        val staleOfferPrice = PriceUtil.fromMarketPriceMarkup(staleMarketPrice, percentage)
        return CreateOfferPresenter.CreateOfferModel().also { m ->
            m.market = market
            m.direction = DirectionEnum.BUY
            m.amountType = CreateOfferPresenter.AmountType.RANGE_AMOUNT
            m.quoteSideMinRangeAmount = with(FiatVOFactory) { fromFaceValue(1000.0, "USD") }
            m.quoteSideMaxRangeAmount = with(FiatVOFactory) { fromFaceValue(5000.0, "USD") }
            m.baseSideMinRangeAmount = with(CoinVOFactory) { fromFaceValue(0.01, "BTC") }
            m.baseSideMaxRangeAmount = with(CoinVOFactory) { fromFaceValue(0.05, "BTC") }
            m.priceType = CreateOfferPresenter.PriceType.PERCENTAGE
            m.percentagePriceValue = percentage
            m.originalPriceQuote = staleMarketPrice
            m.priceQuote = staleOfferPrice
            m.availableQuoteSidePaymentMethods = listOf("REVOLUT")
            m.selectedQuoteSidePaymentMethods = setOf("REVOLUT")
            m.selectedBaseSidePaymentMethods = setOf("MAIN_CHAIN")
        }
    }

    // --- Tests ---

    /**
     * When the review screen opens with percentage-based pricing, and the market price
     * has changed since the price step, the displayed offer price should be recalculated
     * using the CURRENT market price, not the stale one stored in the model.
     */
    @Test
    fun `when percentage pricing with stale market price then formattedPrice uses current market price`() {
        val marketUSD = MarketVOFactory.USD
        // Stale market price from when user selected the market (e.g., $100,000)
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        // Current market price has moved to $105,000
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        // Model has stale data: price was calculated with $100,000 + 10% = $110,000
        val percentage = 0.10
        createOfferPresenter.createOfferModel =
            buildModelWithPercentagePricing(marketUSD, staleMarketPrice, percentage)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // Expected: price recalculated from CURRENT market ($105,000) + 10% = $115,500
        val expectedPrice = PriceUtil.fromMarketPriceMarkup(currentMarketItem.priceQuote, percentage)
        val expectedFormattedPrice = PriceQuoteFormatter.format(expectedPrice, true, false)

        // The stale price would be $100,000 * 1.10 = $110,000
        val staleFormattedPrice =
            PriceQuoteFormatter.format(
                PriceUtil.fromMarketPriceMarkup(staleMarketPrice, percentage),
                true,
                false,
            )

        assertEquals(expectedFormattedPrice, reviewPresenter.formattedPrice)
        assertNotEquals(staleFormattedPrice, reviewPresenter.formattedPrice)
    }

    /**
     * When the review screen opens with fixed pricing, the displayed price should
     * remain the user's chosen fixed price, regardless of market price changes.
     */
    @Test
    fun `when fixed pricing then formattedPrice stays at fixed value`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val fixedPrice = with(PriceQuoteVOFactory) { fromPrice(110000.0, marketUSD) }
        // Market has moved to $105,000 but fixed price should stay at $110,000
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        // Fixed price at $110,000 (was 10% above $100,000 when entered)
        createOfferPresenter.createOfferModel =
            buildModelWithFixedPricing(marketUSD, staleMarketPrice, fixedPrice, 0.10)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // Fixed price should always show the user's chosen price
        val expectedFormattedPrice = PriceQuoteFormatter.format(fixedPrice, true, false)
        assertEquals(expectedFormattedPrice, reviewPresenter.formattedPrice)
    }

    /**
     * When the review screen opens with percentage pricing at 0% (market price),
     * the displayed price should be the current market price, not the stale one.
     */
    @Test
    fun `when zero percent pricing then formattedPrice uses current market price`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        // 0% pricing: offer price = market price
        createOfferPresenter.createOfferModel =
            buildModelWithPercentagePricing(marketUSD, staleMarketPrice, 0.0)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // Should show current market price ($105,000), not stale ($100,000)
        val expectedFormattedPrice =
            PriceQuoteFormatter.format(currentMarketItem.priceQuote, true, false)
        val staleFormattedPrice = PriceQuoteFormatter.format(staleMarketPrice, true, false)

        assertEquals(expectedFormattedPrice, reviewPresenter.formattedPrice)
        assertNotEquals(staleFormattedPrice, reviewPresenter.formattedPrice)
    }

    /**
     * When the review screen opens with percentage pricing and the market price
     * has changed, the priceDetails text should reference the current market price,
     * not the stale originalPriceQuote.
     */
    @Test
    fun `when percentage pricing then priceDetails references current market price`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        createOfferPresenter.createOfferModel =
            buildModelWithPercentagePricing(marketUSD, staleMarketPrice, 0.10)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // The priceDetails should contain the current market price, not the stale one
        val currentMarketFormatted =
            PriceQuoteFormatter.format(currentMarketItem.priceQuote, true, true)
        val staleMarketFormatted = PriceQuoteFormatter.format(staleMarketPrice, true, true)

        assert(reviewPresenter.priceDetails.contains(currentMarketFormatted)) {
            "priceDetails should contain current market price '$currentMarketFormatted' " +
                "but was: '${reviewPresenter.priceDetails}'"
        }
        assert(!reviewPresenter.priceDetails.contains(staleMarketFormatted)) {
            "priceDetails should NOT contain stale market price '$staleMarketFormatted' " +
                "but was: '${reviewPresenter.priceDetails}'"
        }
    }

    /**
     * When percentage pricing with a negative percentage (below market),
     * the priceDetails should contain "below".
     */
    @Test
    fun `when percentage pricing below market then priceDetails contains below`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        // Negative percentage = below market price
        createOfferPresenter.createOfferModel =
            buildModelWithPercentagePricing(marketUSD, staleMarketPrice, -0.05)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // priceDetails should mention "below"
        assert(reviewPresenter.priceDetails.contains("below", ignoreCase = true)) {
            "priceDetails should contain 'below' for negative percentage but was: '${reviewPresenter.priceDetails}'"
        }
    }

    /**
     * When using range amount with percentage pricing, the presenter should
     * populate formattedBaseRangeMinAmount and formattedBaseRangeMaxAmount.
     */
    @Test
    fun `when range amount with percentage pricing then range amounts are populated`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        createOfferPresenter.createOfferModel =
            buildModelWithRangeAmountPercentagePricing(marketUSD, staleMarketPrice, 0.10)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        assert(reviewPresenter.isRangeOffer) { "isRangeOffer should be true for RANGE_AMOUNT" }
        assert(reviewPresenter.formattedBaseRangeMinAmount.isNotEmpty()) {
            "formattedBaseRangeMinAmount should not be empty"
        }
        assert(reviewPresenter.formattedBaseRangeMaxAmount.isNotEmpty()) {
            "formattedBaseRangeMaxAmount should not be empty"
        }
    }

    /**
     * When fixed pricing equals the current market price (0% delta),
     * the priceDetails should contain "atMarket"-style text.
     */
    @Test
    fun `when fixed pricing at market price then priceDetails shows at market`() {
        val marketUSD = MarketVOFactory.USD
        val currentPrice = 105000.0
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val fixedPrice = with(PriceQuoteVOFactory) { fromPrice(currentPrice, marketUSD) }
        val currentMarketItem = makeMarketPriceItem(marketUSD, currentPrice)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        // Fixed price equals current market → percentage delta is 0
        createOfferPresenter.createOfferModel =
            buildModelWithFixedPricing(marketUSD, staleMarketPrice, fixedPrice, 0.0)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // Should use the "atMarket" i18n key path (line 215-216)
        val currentMarketFormatted =
            PriceQuoteFormatter.format(currentMarketItem.priceQuote, true, true)
        assert(reviewPresenter.priceDetails.contains(currentMarketFormatted)) {
            "priceDetails for fixed-at-market should reference market price '$currentMarketFormatted' " +
                "but was: '${reviewPresenter.priceDetails}'"
        }
    }

    /**
     * When fixed pricing is below the current market price,
     * the priceDetails should contain "below".
     */
    @Test
    fun `when fixed pricing below market then priceDetails contains below`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        // Fixed price at $100,000 but market has moved to $105,000 → below market
        val fixedPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val currentMarketItem = makeMarketPriceItem(marketUSD, 105000.0)
        val prices = mutableMapOf(marketUSD to currentMarketItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        createOfferPresenter.createOfferModel =
            buildModelWithFixedPricing(marketUSD, staleMarketPrice, fixedPrice, -0.05)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        assert(reviewPresenter.priceDetails.contains("below", ignoreCase = true)) {
            "priceDetails should contain 'below' for fixed price below market but was: '${reviewPresenter.priceDetails}'"
        }
    }

    /**
     * When market price is unavailable, the presenter should fall back to
     * stored model values without crashing.
     */
    @Test
    fun `when market price unavailable then falls back to stored values`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        // Empty prices map → findMarketPriceItem returns null → getMostRecentPriceQuote throws
        val prices = mutableMapOf<MarketVO, MarketPriceItem>()
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        val percentage = 0.10
        createOfferPresenter.createOfferModel =
            buildModelWithPercentagePricing(marketUSD, staleMarketPrice, percentage)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        // Should not throw even with no market price available
        reviewPresenter.onViewAttached()

        // Falls back to stored priceQuote
        val expectedFallbackPrice = createOfferPresenter.createOfferModel.priceQuote
        val expectedFormatted = PriceQuoteFormatter.format(expectedFallbackPrice, true, false)
        assertEquals(expectedFormatted, reviewPresenter.formattedPrice)
    }

    /**
     * When fixed pricing and market price is unavailable, the priceDetails
     * should fall back to the stored percentage value.
     */
    @Test
    fun `when fixed pricing and no market price then uses stored percentage`() {
        val marketUSD = MarketVOFactory.USD
        val staleMarketPrice = with(PriceQuoteVOFactory) { fromPrice(100000.0, marketUSD) }
        val fixedPrice = with(PriceQuoteVOFactory) { fromPrice(110000.0, marketUSD) }
        val prices = mutableMapOf<MarketVO, MarketPriceItem>()
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic(
            "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt",
        )
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)

        createOfferPresenter.createOfferModel =
            buildModelWithFixedPricing(marketUSD, staleMarketPrice, fixedPrice, 0.10)

        val reviewPresenter = CreateOfferReviewPresenter(mainPresenter, createOfferPresenter)
        reviewPresenter.onViewAttached()

        // Should use stale originalPriceQuote for display since currentMarketPrice is null
        val staleMarketFormatted = PriceQuoteFormatter.format(staleMarketPrice, true, true)
        assert(reviewPresenter.priceDetails.contains(staleMarketFormatted)) {
            "priceDetails should fall back to stored market price '$staleMarketFormatted' " +
                "but was: '${reviewPresenter.priceDetails}'"
        }
    }
}
