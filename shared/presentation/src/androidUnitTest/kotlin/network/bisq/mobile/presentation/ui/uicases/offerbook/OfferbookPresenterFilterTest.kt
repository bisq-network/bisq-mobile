package network.bisq.mobile.presentation.ui.uicases.offerbook

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.getScreenWidthDp
import network.bisq.mobile.presentation.notification.ForegroundServiceController
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.ui.navigation.manager.NavigationManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterFilterTest {

    private val testDispatcher = StandardTestDispatcher()

    // Minimal Koin setup to satisfy BasePresenter injections
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    // Test CoroutineJobsManager uses the provided dispatcher for both UI and IO
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    // Navigation is not exercised in these tests, but BasePresenter injects it lazily
                    single<NavigationManager> { NoopNavigationManager() }
                }
            )
        }
        // Avoid touching Android-specific density in MainPresenter.init
        mockkStatic("network.bisq.mobile.presentation.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private class FakeForegroundDetector : network.bisq.mobile.domain.service.ForegroundDetector {
        private val _isForeground = MutableStateFlow(true)
        override val isForeground: StateFlow<Boolean> = _isForeground
    }

    @Test
    fun onlyMyOffers_and_emptySelection_semantics_and_baseline_stability() = runTest(testDispatcher) {
        // --- Mocks and fakes for MainPresenter ---
        val tradesServiceFacade = mockk<TradesServiceFacade>()
        every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())

        val userProfileServiceForMain = FakeUserProfileServiceFacade()

        val openTradesNotificationService = mockk<OpenTradesNotificationService>(relaxed = true)

        val settingsService = FakeSettingsServiceFacade()
        val tradeReadStateRepository = FakeTradeReadStateRepository()
        val urlLauncher = mockk<UrlLauncher>(relaxed = true)

        val mainPresenter = MainPresenter(
            tradesServiceFacade,
            userProfileServiceForMain,
            openTradesNotificationService,
            settingsService,
            tradeReadStateRepository,
            urlLauncher
        )

        // --- Dependencies for OfferbookPresenter ---
        val offersFlow = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
        val marketFlow = MutableStateFlow(
            OfferbookMarket(MarketVO(baseCurrencyCode = "BTC", quoteCurrencyCode = "USD", baseCurrencyName = "Bitcoin", quoteCurrencyName = "US Dollar"))
        )
        val offersService = mockk<OffersServiceFacade>()
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns marketFlow
        coEvery { offersService.deleteOffer(any()) } returns Result.success(true)

        // Direction SELL -> mirror is BUY (default presenter's selectedDirection)
        fun makeOffer(
            id: String,
            isMy: Boolean,
            quoteMethods: List<String>,
            baseMethods: List<String>,
            makerId: String = id
        ): OfferItemPresentationModel {
            val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
            val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L)
            val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) })
            val makerNetworkId = NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = makerId, hash = makerId, id = makerId)
            )
            val offer = BisqEasyOfferVO(
                id = id,
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.SELL, // mirror -> BUY
                market = market,
                amountSpec = amountSpec,
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = listOf("en")
            )
            val user: UserProfileVO = createMockUserProfile("maker-$makerId")
            val reputation = ReputationScoreVO(0, 0.0, 0)
            val dto = OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = isMy,
                userProfile = user,
                formattedDate = "",
                formattedQuoteAmount = "",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = quoteMethods,
                baseSidePaymentMethods = baseMethods,
                reputationScore = reputation
            )
            return OfferItemPresentationModel(dto)
        }

        val allOffers = listOf(
            makeOffer("o1", isMy = true, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
            makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
            makeOffer("o3", isMy = false, quoteMethods = listOf("CASH_APP"), baseMethods = listOf("MAIN_CHAIN")),
        )
        offersFlow.value = allOffers

        // User profile facade for OfferbookPresenter
        val offerUserProfileService = mockk<UserProfileServiceFacade>(relaxed = true)
        val me = createMockUserProfile("me")
        every { offerUserProfileService.selectedUserProfile } returns MutableStateFlow(me)
        coEvery { offerUserProfileService.isUserIgnored(any()) } returns false
        coEvery { offerUserProfileService.getUserProfileIcon(any(), any()) } returns mockk(relaxed = true)
        coEvery { offerUserProfileService.getUserProfileIcon(any()) } returns mockk(relaxed = true)

        // Market price and reputation services are not exercised (SELL offers skip rep check)
        val marketPriceServiceFacade = object : MarketPriceServiceFacade(mockk(relaxed = true)) {
            override fun findMarketPriceItem(marketVO: MarketVO) = null
            override fun findUSDMarketPriceItem() = null
            override fun refreshSelectedFormattedMarketPrice() {}
            override fun selectMarket(marketListItem: network.bisq.mobile.domain.data.model.offerbook.MarketListItem) {}
        }
        val reputationService = mockk<ReputationServiceFacade>(relaxed = true)

        val takeOfferPresenter = mockk<network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter>(relaxed = true)
        val createOfferPresenter = mockk<network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter>(relaxed = true)

        val presenter = OfferbookPresenter(
            mainPresenter,
            offersService,
            takeOfferPresenter,
            createOfferPresenter,
            marketPriceServiceFacade,
            offerUserProfileService,
            reputationService
        )
        presenter.onViewAttached()
        runCurrent()

        // helper: await baseline availability to be populated by presenter combine (runs on test dispatcher)
        suspend fun awaitBaseline(expectedPay: Set<String>, expectedSettle: Set<String>) {
            repeat(20) {
                runCurrent()
                val pay = presenter.availablePaymentMethodIds.value
                val sett = presenter.availableSettlementMethodIds.value
                if (pay == expectedPay && sett == expectedSettle) return
            }
            // fall-through: let asserts show mismatch if any
        }

        // helper: await until the sorted result count matches (processing uses IO/Main in presenter)
        suspend fun awaitSortedCount(expected: Int) {
            presenter.sortedFilteredOffers
                .filter { it.size == expected }
                .first()
        }

        val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
        val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
        awaitBaseline(expectedPayments, expectedSettlements)

        // Act: attach and let initial combine run
        presenter.onViewAttached()
        runCurrent()

        // Baseline availability should reflect all offers (direction+ignored-user filtered)
        val availablePayments = presenter.availablePaymentMethodIds.value
        val availableSettlements = presenter.availableSettlementMethodIds.value
        assertEquals(expectedPayments, availablePayments)
        assertEquals(expectedSettlements, availableSettlements)

        println("[TEST] baseline available payments=" + availablePayments + ", settlements=" + availableSettlements)

        // Initialize selections to all available (UI does this on first render)
        presenter.setSelectedPaymentMethodIds(availablePayments)
        presenter.setSelectedSettlementMethodIds(availableSettlements)
        awaitSortedCount(allOffers.size)

        // With all selected and onlyMy=false, we should see all offers
        assertEquals(allOffers.size, presenter.sortedFilteredOffers.value.size)

        println("[TEST] after select: filteredCount=" + presenter.sortedFilteredOffers.value.size)

        // Enable Only My Offers -> should filter down to my single offer
        presenter.setOnlyMyOffers(true)
        awaitSortedCount(1)
        assertEquals(1, presenter.sortedFilteredOffers.value.size)
        assertTrue(presenter.sortedFilteredOffers.value.all { it.isMyOffer })

        // Disable Only My Offers -> all offers again
        presenter.setOnlyMyOffers(false)
        awaitSortedCount(allOffers.size)
        assertEquals(allOffers.size, presenter.sortedFilteredOffers.value.size)

        // Empty selection semantics: empty = exclude all
        presenter.setSelectedPaymentMethodIds(emptySet())
        awaitSortedCount(0)
        assertEquals(0, presenter.sortedFilteredOffers.value.size)
        // Baseline availability remains stable
        assertEquals(setOf("SEPA", "CASH_APP"), presenter.availablePaymentMethodIds.value)

        presenter.setSelectedSettlementMethodIds(emptySet())
        awaitSortedCount(0)
        assertEquals(0, presenter.sortedFilteredOffers.value.size)
        assertEquals(setOf("MAIN_CHAIN", "LIGHTNING"), presenter.availableSettlementMethodIds.value)

        // Restore selections -> offers visible again
        presenter.setSelectedPaymentMethodIds(availablePayments)
        presenter.setSelectedSettlementMethodIds(availableSettlements)
        awaitSortedCount(allOffers.size)
        assertEquals(allOffers.size, presenter.sortedFilteredOffers.value.size)
    }

    // --- Minimal helpers/types for tests ---

    private class TestCoroutineJobsManager(private val dispatcher: CoroutineDispatcher) : CoroutineJobsManager {
        private val uiScope = CoroutineScope(dispatcher + SupervisorJob())
        private val ioScope = CoroutineScope(dispatcher + SupervisorJob())
        private val jobs = mutableSetOf<Job>()
        override fun addJob(job: Job): Job { jobs += job; job.invokeOnCompletion { jobs -= job }; return job }
        override fun launchUI(context: CoroutineContext, block: suspend CoroutineScope.() -> Unit): Job =
            addJob(uiScope.launch(context) { block() })
        override fun launchIO(block: suspend CoroutineScope.() -> Unit): Job = addJob(ioScope.launch { block() })
        override fun <T> collectUI(flow: Flow<T>, collector: suspend (T) -> Unit): Job = launchUI { flow.collect { collector(it) } }
        override fun <T> collectIO(flow: Flow<T>, collector: suspend (T) -> Unit): Job = launchIO { flow.collect { collector(it) } }
        override suspend fun dispose() { uiScope.cancel(); ioScope.cancel(); jobs.clear() }
        override fun getUIScope(): CoroutineScope = uiScope
        override fun getIOScope(): CoroutineScope = ioScope
        override fun setCoroutineExceptionHandler(handler: (Throwable) -> Unit) {}
    }

    private class NoopNavigationManager : NavigationManager {
        private val _currentTab = MutableStateFlow<TabNavRoute?>(null)
        override val currentTab: StateFlow<TabNavRoute?> get() = _currentTab.asStateFlow()
        override fun setRootNavController(navController: androidx.navigation.NavHostController?) {}
        override fun setTabNavController(navController: androidx.navigation.NavHostController?) {}
        override fun isAtMainScreen(): Boolean = true
        override fun isAtHomeTab(): Boolean = true
        override fun showBackButton(): Boolean = false
        override fun navigate(destination: NavRoute, customSetup: (androidx.navigation.NavOptionsBuilder) -> Unit, onCompleted: (() -> Unit)?) { onCompleted?.invoke() }
        override fun navigateToTab(destination: TabNavRoute, saveStateOnPopUp: Boolean, shouldLaunchSingleTop: Boolean, shouldRestoreState: Boolean) { _currentTab.value = destination }
        override fun navigateBackTo(destination: NavRoute, shouldInclusive: Boolean, shouldSaveState: Boolean) {}
        override fun navigateFromUri(uri: String) {}
        override fun navigateBack(onCompleted: (() -> Unit)?) { onCompleted?.invoke() }
    }

    private class FakeSettingsServiceFacade : SettingsServiceFacade {
        override suspend fun getSettings() = Result.success(network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj)
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

    private class FakeTradeReadStateRepository : TradeReadStateRepository {
        override val data: Flow<TradeReadStateMap> = flowOf(TradeReadStateMap())
        override suspend fun setCount(tradeId: String, count: Int) {}
        override suspend fun clearId(tradeId: String) {}
    }

    private class FakeUserProfileServiceFacade : UserProfileServiceFacade {
        override val selectedUserProfile: StateFlow<network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO?> = MutableStateFlow(null)
        override val ignoredProfileIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override val numUserProfiles: StateFlow<Int> = MutableStateFlow(1)
        override suspend fun hasUserProfile(): Boolean = true
        override suspend fun generateKeyPair(imageSize: Int, result: (String, String, network.bisq.mobile.domain.PlatformImage?) -> Unit) {}
        override suspend fun createAndPublishNewUserProfile(nickName: String) {}
        override suspend fun updateAndPublishUserProfile(statement: String?, terms: String?) = Result.success(createMockUserProfile("me"))
        override suspend fun getUserIdentityIds(): List<String> = emptyList()
        override suspend fun applySelectedUserProfile(): Triple<String?, String?, String?> = Triple(null, null, null)
        override suspend fun getSelectedUserProfile() = createMockUserProfile("me")
        override suspend fun findUserProfile(profileId: String) = createMockUserProfile(profileId)
        override suspend fun findUserProfiles(ids: List<String>) = ids.map { createMockUserProfile(it) }
        override suspend fun getUserProfileIcon(userProfile: network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO, size: Number) = createEmptyImage()
        override suspend fun getUserProfileIcon(userProfile: network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO) = createEmptyImage()
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

}

