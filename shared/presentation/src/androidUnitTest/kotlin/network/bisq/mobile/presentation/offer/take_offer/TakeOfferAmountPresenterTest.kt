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
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.amount.TakeOfferAmountPresenter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO as OfferVO

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferAmountPresenterTest {
    // --- Fakes (Android/JVM-friendly) ---
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDownMainDispatcher() {
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
            bisqEasyOffer: OfferVO,
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

        override suspend fun generateKeyPair(
            imageSize: Int,
            result: (String, String, PlatformImage?) -> Unit,
        ) {
        }

        override suspend fun createAndPublishNewUserProfile(nickName: String) {}

        override suspend fun updateAndPublishUserProfile(
            statement: String?,
            terms: String?,
        ) = Result.success(createMockUserProfile("me"))

        override suspend fun getUserIdentityIds(): List<String> = emptyList()

        override suspend fun applySelectedUserProfile(): Triple<String?, String?, String?> = Triple(null, null, null)

        override suspend fun getSelectedUserProfile() = createMockUserProfile("me")

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
        ) {
        }

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

    private fun makeOfferDto(): OfferItemPresentationDto {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L) // $10.0000 .. $100.0000
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) }) // 100 USD per BTC
        val makerNetworkId = NetworkIdVO(AddressByTransportTypeMapVO(mapOf()), PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"))
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
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
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
            quoteSidePaymentMethods = listOf("SEPA"),
            baseSidePaymentMethods = listOf("BTC"),
            reputationScore = reputation,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onSliderValueChanged_is_sampled_and_onSliderDragFinished_updates_immediately() =
        runTest {
            // Arrange market prices map (100 USD per BTC)
            val marketUSD = MarketVOFactory.USD
            val marketUSDItem =
                MarketPriceItem(
                    marketUSD,
                    with(PriceQuoteVOFactory) { fromPrice(100_00L, marketUSD) },
                    formattedPrice = "100 USD",
                )
            val prices = mapOf(marketUSD to marketUSDItem)
            val settingsRepo = FakeSettingsRepository()
            val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, prices)

            // Mock top-level android-specific function called from MainPresenter.init
            mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
            every { getScreenWidthDp() } returns 480

            val mainPresenter = makeMainPresenter()
            val tradesServiceFacade = FakeTradesServiceFacade()
            val takeOfferPresenter =
                TakeOfferPresenter(mainPresenter, marketPriceServiceFacade, tradesServiceFacade)

            // Select offer
            val dto = makeOfferDto()
            val model = OfferItemPresentationModel(dto)
            takeOfferPresenter.selectOfferToTake(model)

            // Create Amount presenter (runs init)
            val presenter =
                TakeOfferAmountPresenter(mainPresenter, marketPriceServiceFacade, takeOfferPresenter)

            val beforeQuote = presenter.formattedQuoteAmount.value
            val beforeBase = presenter.formattedBaseAmount.value

            // Act: drag updates schedule a sampled heavy update; not immediate
            presenter.onSliderValueChanged(0.75f)
            val midQuote = presenter.formattedQuoteAmount.value
            val midBase = presenter.formattedBaseAmount.value
            assertEquals(beforeQuote, midQuote)
            assertEquals(beforeBase, midBase)
            assertTrue(presenter.amountValid.value)

            // After debounce window, sampled update should refresh values
            advanceTimeBy(40)
            runCurrent()
            val afterSampleQuote = presenter.formattedQuoteAmount.value
            val afterSampleBase = presenter.formattedBaseAmount.value
            assertNotEquals(beforeQuote, afterSampleQuote)
            assertNotEquals(beforeBase, afterSampleBase)

            // Another drag then immediate release triggers immediate update (no extra wait)
            val beforeReleaseQuote = presenter.formattedQuoteAmount.value
            val beforeReleaseBase = presenter.formattedBaseAmount.value
            presenter.onSliderValueChanged(0.80f)
            presenter.onSliderDragFinished()
            val afterReleaseQuote = presenter.formattedQuoteAmount.value
            val afterReleaseBase = presenter.formattedBaseAmount.value
            assertNotEquals(beforeReleaseQuote, afterReleaseQuote)
            assertNotEquals(beforeReleaseBase, afterReleaseBase)
        }
}
