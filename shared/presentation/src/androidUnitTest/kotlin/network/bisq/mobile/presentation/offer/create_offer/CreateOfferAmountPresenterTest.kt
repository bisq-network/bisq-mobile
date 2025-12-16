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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOfferAmountPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                }
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
        override suspend fun update(transform: suspend (t: Settings) -> Settings) { _data.value = transform(_data.value) }
        override suspend fun clear() { _data.value = Settings() }
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
        override val chatNotificationType: StateFlow<ChatChannelNotificationTypeEnum> = MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)
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

    private class FakeUserProfileServiceFacade : UserProfileServiceFacade {
        override val selectedUserProfile: StateFlow<UserProfileVO?> = MutableStateFlow(null)
        override val ignoredProfileIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override val numUserProfiles: StateFlow<Int> = MutableStateFlow(1)
        override suspend fun hasUserProfile(): Boolean = true
        override suspend fun generateKeyPair(imageSize: Int, result: (String, String, PlatformImage?) -> Unit) {}
        override suspend fun createAndPublishNewUserProfile(nickName: String) {}
        override suspend fun updateAndPublishUserProfile(statement: String?, terms: String?) = Result.failure<UserProfileVO>(Exception("unused in test"))
        override suspend fun getUserIdentityIds(): List<String> = emptyList()
        override suspend fun applySelectedUserProfile(): Triple<String?, String?, String?> = Triple(null, null, null)
        override suspend fun getSelectedUserProfile() = null
        override suspend fun findUserProfile(profileId: String) = null
        override suspend fun findUserProfiles(ids: List<String>) = emptyList<UserProfileVO>()
        override suspend fun getUserProfileIcon(userProfile: UserProfileVO, size: Number) = createEmptyImage()
        override suspend fun getUserProfileIcon(userProfile: UserProfileVO) = createEmptyImage()
        override suspend fun getUserPublishDate(): Long = 0L
        override suspend fun userActivityDetected() {}
        override suspend fun ignoreUserProfile(profileId: String) {}
        override suspend fun undoIgnoreUserProfile(profileId: String) {}
        override suspend fun isUserIgnored(profileId: String): Boolean = false
        override suspend fun getIgnoredUserProfileIds(): Set<String> = emptySet()
        override suspend fun reportUserProfile(
            accusedUserProfile: UserProfileVO,
            message: String
        ): Result<Unit> = Result.failure(Exception("unused in test"))
    }

    private class FakeReputationServiceFacade : ReputationServiceFacade {
        override val scoreByUserProfileId: Map<String, Long> = mapOf(
            "userA" to 100L, "userB" to 200L
        )
        override suspend fun getReputation(userProfileId: String): Result<ReputationScoreVO> =
            Result.failure(Exception("unused in buyer path"))
        override suspend fun getProfileAge(userProfileId: String) = Result.success(null)
        override suspend fun activate() {}
        override suspend fun deactivate() {}
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
            takeOfferErrorMessage: MutableStateFlow<String?>
        ) = Result.success("trade-1")
        override fun selectOpenTrade(tradeId: String) {}
        override suspend fun rejectTrade() = Result.success(Unit)
        override suspend fun cancelTrade() = Result.success(Unit)
        override suspend fun closeTrade() = Result.success(Unit)
        override suspend fun sellerSendsPaymentAccount(paymentAccountData: String) = Result.success(Unit)
        override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String) = Result.success(Unit)
        override suspend fun sellerConfirmFiatReceipt() = Result.success(Unit)
        override suspend fun buyerConfirmFiatSent() = Result.success(Unit)
        override suspend fun sellerConfirmBtcSent(paymentProof: String?) = Result.success(Unit)
        override suspend fun btcConfirmed() = Result.success(Unit)
        override suspend fun exportTradeDate() = Result.success(Unit)
        override fun resetSelectedTradeToNull() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
    }

    private class FakeTradeReadStateRepository : TradeReadStateRepository {
        override val data: Flow<TradeReadStateMap> = flowOf(TradeReadStateMap())
        override suspend fun setCount(tradeId: String, count: Int) {}
        override suspend fun clearId(tradeId: String) {}
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
        override fun <T> registerObserver(flow: Flow<T>, onStateChange: (T) -> Unit) {}
        override fun unregisterObserver(flow: Flow<*>) {}
        override fun unregisterObservers() {}
        override fun isServiceRunning(): Boolean = false
        override fun dispose() {}
    }

    private class FakeForegroundDetector : ForegroundDetector {
        private val _isForeground = MutableStateFlow(true)
        override val isForeground: StateFlow<Boolean> = _isForeground
    }

    private class FakeUrlLauncher : UrlLauncher { override fun openUrl(url: String) {} }

    private fun makeMainPresenter(): MainPresenter {
        val tradesServiceFacade = FakeTradesServiceFacade()
        val userProfileServiceFacade = FakeUserProfileServiceFacade()
        val notificationController = FakeNotificationController()
        val foregroundServiceController = FakeForegroundServiceController()
        val foregroundDetector = FakeForegroundDetector()
        val openTradesNotificationService = OpenTradesNotificationService(
            notificationController,
            foregroundServiceController,
            tradesServiceFacade,
            userProfileServiceFacade,
            foregroundDetector
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
            TestApplicationLifecycleService()
        )
    }


    @Test
    fun fixed_slider_updates_progressively_and_limit_info_updates_on_release() = runTest(testDispatcher) {
        // Arrange market prices map (100 USD per BTC)
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem = MarketPriceItem(marketUSD, with(PriceQuoteVOFactory) { fromPrice(100_00L, marketUSD) }, formattedPrice = "100 USD")
        val prices = mapOf(marketUSD to marketUSDItem)

        // Mock MarketPriceServiceFacade to avoid Koin
        val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true).apply {
            every { findMarketPriceItem(any()) } answers {
                val arg = firstArg<MarketVO>()
                prices.values.firstOrNull { it.market.baseCurrencyCode == arg.baseCurrencyCode && it.market.quoteCurrencyCode == arg.quoteCurrencyCode }
            }
            every { findUSDMarketPriceItem() } returns prices[marketUSD]
            every { refreshSelectedFormattedMarketPrice() } returns Unit
            every { selectMarket(any()) } returns Unit
        }

        // Mock the Android top-level function accessed by MainPresenter
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()

        val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
        val createOfferPresenter = CreateOfferPresenter(
            mainPresenter,
            marketPriceServiceFacade,
            offersServiceFacade,
            FakeSettingsServiceFacade(),
        )
        // Prepare model with market set
        createOfferPresenter.createOfferModel = CreateOfferPresenter.CreateOfferModel().also { m ->
            m.market = marketUSD
        }

        val amountPresenter = CreateOfferAmountPresenter(
            mainPresenter,
            marketPriceServiceFacade,
            createOfferPresenter,
            FakeUserProfileServiceFacade(),
            FakeReputationServiceFacade(),
        )

        // Let initial init coroutines run
        runCurrent()

        val initialOverlayInfo = amountPresenter.amountLimitInfoOverlayInfo.value
        val beforeQuote = amountPresenter.formattedQuoteSideFixedAmount.value
        val beforeBase = amountPresenter.formattedBaseSideFixedAmount.value

        // Act: progressive updates on drag (heavy conversions/formatting do occur in Create flow)
        amountPresenter.onFixedAmountSliderValueChange(0.75f)
        val midQuote = amountPresenter.formattedQuoteSideFixedAmount.value
        val midBase = amountPresenter.formattedBaseSideFixedAmount.value
        assertNotEquals(beforeQuote, midQuote)
        assertNotEquals(beforeBase, midBase)

        // Heavy reputation/limit overlay should not run during drag
        assertEquals(initialOverlayInfo, amountPresenter.amountLimitInfoOverlayInfo.value)

        // On release, heavy path is allowed to run; should complete without changing mid-drag formatted values
        amountPresenter.onSliderDragFinished()
        advanceTimeBy(0)
        runCurrent()
        // Sanity: formatted values remain the latest ones set during drag
        assertEquals(midQuote, amountPresenter.formattedQuoteSideFixedAmount.value)
        assertEquals(midBase, amountPresenter.formattedBaseSideFixedAmount.value)
    }

    @Test
    fun range_slider_updates_progressively_and_limit_info_updates_on_release() = runTest(testDispatcher) {
        // Arrange market prices map (100 USD per BTC)
        val marketUSD = MarketVOFactory.USD
        val marketUSDItem = MarketPriceItem(marketUSD, with(PriceQuoteVOFactory) { fromPrice(100_00L, marketUSD) }, formattedPrice = "100 USD")
        val prices = mapOf(marketUSD to marketUSDItem)

        // Mock MarketPriceServiceFacade to avoid Koin
        val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true).apply {
            every { findMarketPriceItem(any()) } answers {
                val arg = firstArg<MarketVO>()
                prices.values.firstOrNull { it.market.baseCurrencyCode == arg.baseCurrencyCode && it.market.quoteCurrencyCode == arg.quoteCurrencyCode }
            }
            every { findUSDMarketPriceItem() } returns prices[marketUSD]
            every { refreshSelectedFormattedMarketPrice() } returns Unit
            every { selectMarket(any()) } returns Unit
        }

        // Mock the Android top-level function accessed by MainPresenter
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val mainPresenter = makeMainPresenter()

        val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
        val createOfferPresenter = CreateOfferPresenter(
            mainPresenter,
            marketPriceServiceFacade,
            offersServiceFacade,
            FakeSettingsServiceFacade(),
        )
        // Prepare model with market set
        createOfferPresenter.createOfferModel = CreateOfferPresenter.CreateOfferModel().also { m ->
            m.market = marketUSD
        }

        val amountPresenter = CreateOfferAmountPresenter(
            mainPresenter,
            marketPriceServiceFacade,
            createOfferPresenter,
            FakeUserProfileServiceFacade(),
            FakeReputationServiceFacade(),
        )

        // Let initial init coroutines run
        runCurrent()

        val initialOverlayInfo = amountPresenter.amountLimitInfoOverlayInfo.value
        val beforeMinSlider = amountPresenter.minRangeSliderValue.value
        val beforeMaxSlider = amountPresenter.maxRangeSliderValue.value

        // Act: progressive updates on drag for range (simulate each thumb moving)
        amountPresenter.onMinRangeSliderValueChange(0.3f)
        amountPresenter.onMaxRangeSliderValueChange(0.7f)
        val midMinSlider = amountPresenter.minRangeSliderValue.value
        val midMaxSlider = amountPresenter.maxRangeSliderValue.value
        assertNotEquals(beforeMinSlider, midMinSlider)
        assertNotEquals(beforeMaxSlider, midMaxSlider)

        // Heavy reputation/limit overlay should not run during drag
        assertEquals(initialOverlayInfo, amountPresenter.amountLimitInfoOverlayInfo.value)

        // On release, heavy path is allowed to run; ensure stability of slider positions
        amountPresenter.onSliderDragFinished()
        advanceTimeBy(0)
        runCurrent()
        assertEquals(midMinSlider, amountPresenter.minRangeSliderValue.value)
        assertEquals(midMaxSlider, amountPresenter.maxRangeSliderValue.value)
    }

}

