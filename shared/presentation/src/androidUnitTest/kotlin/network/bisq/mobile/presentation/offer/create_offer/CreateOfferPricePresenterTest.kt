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
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
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
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.price.CreateOfferPricePresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOfferPricePresenterTest {
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

    // --- Fakes ---
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

    private class FakeSettingsServiceFacade : SettingsServiceFacade {
        override suspend fun getSettings() = Result.success(settingsVODemoObj)

        override val isTacAccepted: StateFlow<Boolean?> = MutableStateFlow(true)

        override suspend fun confirmTacAccepted(value: Boolean) {}

        override val tradeRulesConfirmed: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun confirmTradeRules(value: Boolean) {}

        override val languageCode: StateFlow<String> = MutableStateFlow("en")

        override suspend fun setLanguageCode(value: String) {}

        override val supportedLanguageCodes: StateFlow<Set<String>> = MutableStateFlow(setOf("en"))

        override suspend fun setSupportedLanguageCodes(value: Set<String>) {}

        override val chatNotificationType: StateFlow<ChatChannelNotificationTypeEnum> =
            MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)

        override suspend fun setChatNotificationType(value: ChatChannelNotificationTypeEnum) {}

        override val closeMyOfferWhenTaken: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setCloseMyOfferWhenTaken(value: Boolean) {}

        override val maxTradePriceDeviation: StateFlow<Double> = MutableStateFlow(0.0)

        override suspend fun setMaxTradePriceDeviation(value: Double) {}

        override val useAnimations: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun setUseAnimations(value: Boolean) {}

        override val difficultyAdjustmentFactor: StateFlow<Double> = MutableStateFlow(1.0)

        override suspend fun setDifficultyAdjustmentFactor(value: Double) {}

        override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean) {}

        override val numDaysAfterRedactingTradeData: StateFlow<Int> = MutableStateFlow(30)

        override suspend fun setNumDaysAfterRedactingTradeData(days: Int) {}
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
        ) = Result.success("trade-1")

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
        ): Result<Unit> = Result.failure(Exception("unused in test"))

        override suspend fun getOwnedUserProfiles(): Result<List<UserProfileVO>> = Result.failure(Exception("unused"))

        override suspend fun selectUserProfile(id: String): Result<UserProfileVO> = Result.failure(Exception("unused"))

        override suspend fun deleteUserProfile(id: String): Result<UserProfileVO> = Result.failure(Exception("unused"))
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

    private fun makePricePresenter(
        mainPresenter: MainPresenter,
        marketPriceServiceFacade: MarketPriceServiceFacade,
        createOfferPresenter: CreateOfferPresenter,
    ): CreateOfferPricePresenter =
        CreateOfferPricePresenter(
            mainPresenter,
            marketPriceServiceFacade,
            createOfferPresenter,
        )

    /**
     * When onFixPriceChanged receives isValid=true from TextField but the actual
     * percentage is out of the allowed [-10%, +50%] range, the presenter should
     * independently set formattedPercentagePriceValid to false.
     */
    @Test
    fun onFixPriceChanged_outOfRange_setsInvalid_evenWhenTextFieldSaysValid() {
        val marketUSD = MarketVOFactory.USD
        val marketPriceQuote = with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) }
        val marketUSDItem = MarketPriceItem(marketUSD, marketPriceQuote, formattedPrice = "100,000 USD")
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)
        createOfferPresenter.createOfferModel =
            CreateOfferPresenter.CreateOfferModel().also { m ->
                m.market = marketUSD
                m.direction = DirectionEnum.BUY
                val mp = MarketPriceSpecVO().getPriceQuoteVO(marketUSDItem)
                m.priceQuote = mp
                m.originalPriceQuote = mp
            }

        val pricePresenter = makePricePresenter(mainPresenter, marketPriceServiceFacade, createOfferPresenter)

        // Verify initial state is valid
        assertTrue(pricePresenter.formattedPercentagePriceValid.value)

        // Pass isValid=true from TextField, but the price is 200% above market ($300,000 vs $100,000)
        pricePresenter.onFixPriceChanged("300000", true)

        // The presenter should independently detect the percentage (2.0 = 200%) exceeds the 50% max
        assertFalse(pricePresenter.formattedPercentagePriceValid.value)
    }

    /**
     * When onFixPriceChanged receives a price within the allowed range,
     * formattedPercentagePriceValid should be true.
     */
    @Test
    fun onFixPriceChanged_withinRange_setsValid() {
        val marketUSD = MarketVOFactory.USD
        val marketPriceQuote = with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) }
        val marketUSDItem = MarketPriceItem(marketUSD, marketPriceQuote, formattedPrice = "100,000 USD")
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)
        createOfferPresenter.createOfferModel =
            CreateOfferPresenter.CreateOfferModel().also { m ->
                m.market = marketUSD
                m.direction = DirectionEnum.BUY
                val mp = MarketPriceSpecVO().getPriceQuoteVO(marketUSDItem)
                m.priceQuote = mp
                m.originalPriceQuote = mp
            }

        val pricePresenter = makePricePresenter(mainPresenter, marketPriceServiceFacade, createOfferPresenter)

        // 10% above market: $110,000
        pricePresenter.onFixPriceChanged("110000", true)

        assertTrue(pricePresenter.formattedPercentagePriceValid.value)
    }

    /**
     * When onFixPriceChanged receives a price that's too far below market (>10% below),
     * formattedPercentagePriceValid should be false.
     */
    @Test
    fun onFixPriceChanged_tooFarBelowMarket_setsInvalid() {
        val marketUSD = MarketVOFactory.USD
        val marketPriceQuote = with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) }
        val marketUSDItem = MarketPriceItem(marketUSD, marketPriceQuote, formattedPrice = "100,000 USD")
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)
        createOfferPresenter.createOfferModel =
            CreateOfferPresenter.CreateOfferModel().also { m ->
                m.market = marketUSD
                m.direction = DirectionEnum.BUY
                val mp = MarketPriceSpecVO().getPriceQuoteVO(marketUSDItem)
                m.priceQuote = mp
                m.originalPriceQuote = mp
            }

        val pricePresenter = makePricePresenter(mainPresenter, marketPriceServiceFacade, createOfferPresenter)

        // 20% below market: $80,000 → percentage = -0.2 → -20% < -10% limit
        pricePresenter.onFixPriceChanged("80000", true)

        assertFalse(pricePresenter.formattedPercentagePriceValid.value)
    }

    /**
     * When onFixPriceChanged receives empty or blank input,
     * formattedPercentagePriceValid should be false.
     */
    @Test
    fun onFixPriceChanged_blankInput_setsInvalid() {
        val marketUSD = MarketVOFactory.USD
        val marketPriceQuote = with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) }
        val marketUSDItem = MarketPriceItem(marketUSD, marketPriceQuote, formattedPrice = "100,000 USD")
        val prices = mapOf(marketUSD to marketUSDItem)
        val settingsRepo = FakeSettingsRepository()
        val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()
        val createOfferPresenter = makeCreateOfferPresenter(mainPresenter, marketPriceServiceFacade)
        createOfferPresenter.createOfferModel =
            CreateOfferPresenter.CreateOfferModel().also { m ->
                m.market = marketUSD
                m.direction = DirectionEnum.BUY
                val mp = MarketPriceSpecVO().getPriceQuoteVO(marketUSDItem)
                m.priceQuote = mp
                m.originalPriceQuote = mp
            }

        val pricePresenter = makePricePresenter(mainPresenter, marketPriceServiceFacade, createOfferPresenter)

        pricePresenter.onFixPriceChanged("", true)

        assertFalse(pricePresenter.formattedPercentagePriceValid.value)
    }
}
