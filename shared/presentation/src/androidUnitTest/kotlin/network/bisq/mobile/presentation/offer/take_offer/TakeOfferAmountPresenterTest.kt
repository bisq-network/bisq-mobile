package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.model.MarketPriceItem
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
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
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
import network.bisq.mobile.presentation.common.test_utils.FakeTradeReadStateRepository
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.amount.TakeOfferAmountPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDownMainDispatcher() {
        stopKoin()
        Dispatchers.resetMain()
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

    private class FakeUserProfileServiceFacade : UserProfileServiceFacade {
        override val userProfiles: StateFlow<List<UserProfileVO>> = MutableStateFlow(emptyList())
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

        override suspend fun getOwnedUserProfiles(): Result<List<UserProfileVO>> = Result.failure(Exception("unused in test"))

        override suspend fun selectUserProfile(id: String): Result<UserProfileVO> = Result.failure(Exception("unused in test"))

        override suspend fun deleteUserProfile(id: String): Result<UserProfileVO> = Result.failure(Exception("unused in test"))
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
        ) {
        }

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
        val amountSpec =
            QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L) // $10.0000 .. $100.0000
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) }) // 100 USD per BTC
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
            )
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
    fun onSliderValueChanged_leading_edge_updates_immediately_and_coalesces_subsequent() =
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
            val settingsRepo = SettingsRepositoryMock()
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

            // Act: leading-edge update fires immediately on first slider interaction
            presenter.onSliderValueChanged(0.75f)
            val midQuote = presenter.formattedQuoteAmount.value
            val midBase = presenter.formattedBaseAmount.value
            assertNotEquals(beforeQuote, midQuote)
            assertNotEquals(beforeBase, midBase)
            assertTrue(presenter.amountValid.value)

            // Subsequent calls within the sample window are coalesced (not applied immediately)
            presenter.onSliderValueChanged(0.95f)
            val coalescedQuote = presenter.formattedQuoteAmount.value
            assertEquals(midQuote, coalescedQuote)

            // After debounce window, coalesced update is applied
            advanceTimeBy(40)
            runCurrent()
            val afterSampleQuote = presenter.formattedQuoteAmount.value
            val afterSampleBase = presenter.formattedBaseAmount.value
            assertNotEquals(midQuote, afterSampleQuote)
            assertNotEquals(midBase, afterSampleBase)

            // Drag finished flushes any pending value immediately
            val beforeReleaseQuote = presenter.formattedQuoteAmount.value
            val beforeReleaseBase = presenter.formattedBaseAmount.value
            presenter.onSliderValueChanged(0.80f)
            presenter.onSliderDragFinished()
            val afterReleaseQuote = presenter.formattedQuoteAmount.value
            val afterReleaseBase = presenter.formattedBaseAmount.value
            assertNotEquals(beforeReleaseQuote, afterReleaseQuote)
            assertNotEquals(beforeReleaseBase, afterReleaseBase)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onTextValueChanged_validInput_updatesAmountsAndSlider() =
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
            val settingsRepo = SettingsRepositoryMock()
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

            // Act: type "50" (which is $50 = 500_000L in minor units)
            presenter.onTextValueChanged("50")
            runCurrent()

            // Assert: amounts are updated, amountValid is true, slider position is within [0,1]
            assertTrue(presenter.formattedQuoteAmount.value.isNotEmpty())
            assertTrue(presenter.formattedBaseAmount.value.isNotEmpty())
            assertTrue(presenter.amountValid.value)
            assertTrue(presenter.sliderPosition.value in 0.0f..1.0f)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onTextValueChanged_outOfRange_setsAmountInvalid() =
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
            val settingsRepo = SettingsRepositoryMock()
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

            // Act: type "1" (which is $1 = 10_000L, below min of ~$6-10)
            presenter.onTextValueChanged("1")
            runCurrent()

            // Assert: amountValid is false
            assertTrue(!presenter.amountValid.value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onTextValueChanged_emptyInput_setsAmountInvalid() =
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
            val settingsRepo = SettingsRepositoryMock()
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

            // Act: type ""
            presenter.onTextValueChanged("")
            runCurrent()

            // Assert: amountValid is false and formattedQuoteAmount is empty
            assertTrue(!presenter.amountValid.value)
            assertTrue(presenter.formattedQuoteAmount.value.isEmpty())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onTextValueChanged_initializationFailed_setsAmountInvalid() =
        runTest {
            // Arrange: Use a market that's not in the prices map to cause initialization failure
            val marketMXN = MarketVO("BTC", "MXN", "Bitcoin", "Mexican Peso")
            val settingsRepo = SettingsRepositoryMock()
            // Empty prices map - no market data available
            val marketPriceServiceFacade = FakeMarketPriceServiceFacade(settingsRepo, emptyMap())

            // Mock top-level android-specific function called from MainPresenter.init
            mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
            every { getScreenWidthDp() } returns 480

            val mainPresenter = makeMainPresenter()
            val tradesServiceFacade = FakeTradesServiceFacade()
            val takeOfferPresenter =
                TakeOfferPresenter(mainPresenter, marketPriceServiceFacade, tradesServiceFacade)

            // Create an offer with MXN market (which has no price data)
            val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L)
            val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, marketMXN) })
            val makerNetworkId = NetworkIdVO(AddressByTransportTypeMapVO(mapOf()), PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"))
            val offer =
                BisqEasyOfferVO(
                    id = "offer-1",
                    date = 0L,
                    makerNetworkId = makerNetworkId,
                    direction = DirectionEnum.BUY,
                    market = marketMXN,
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
            val dto =
                OfferItemPresentationDto(
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

            val model = OfferItemPresentationModel(dto)
            takeOfferPresenter.selectOfferToTake(model)

            // Create Amount presenter (runs init, should fail)
            val presenter =
                TakeOfferAmountPresenter(mainPresenter, marketPriceServiceFacade, takeOfferPresenter)

            // Act: type "50"
            presenter.onTextValueChanged("50")
            runCurrent()

            // Assert: amountValid is false because initialization failed
            assertTrue(!presenter.amountValid.value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onSliderDragFinished_flushesLatestPending() =
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
            val settingsRepo = SettingsRepositoryMock()
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

            // Act: set slider to 0.5, then quickly call onSliderValueChanged(0.6f) then onSliderValueChanged(0.7f)
            // without advancing time (so 0.7 is pending), then call onSliderDragFinished()
            presenter.onSliderValueChanged(0.5f)
            runCurrent() // Apply the leading edge update

            // Quickly change to 0.6f (will be coalesced)
            presenter.onSliderValueChanged(0.6f)
            // Quickly change to 0.7f (will overwrite 0.6f in pending)
            presenter.onSliderValueChanged(0.7f)

            // Without advancing time, call onSliderDragFinished()
            presenter.onSliderDragFinished()
            runCurrent()

            // Assert: the final formattedQuoteAmount should reflect the 0.7f position, not 0.6f or 0.5f
            val finalQuote = presenter.formattedQuoteAmount.value
            val finalSlider = presenter.sliderPosition.value

            // Verify the slider position is close to 0.7f
            assertTrue(kotlin.math.abs(finalSlider - 0.7f) < 0.05f, "Expected slider near 0.7 but got $finalSlider")
            // Verify amounts were updated
            assertTrue(finalQuote.isNotEmpty())
            assertTrue(presenter.amountValid.value)
        }
}
