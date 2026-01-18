package network.bisq.mobile.presentation.offerbook

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
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
                },
            )
        }
        // Avoid touching Android-specific density in MainPresenter.init
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    @AfterTest
    fun tearDown() {
        // Restore any static mocks to prevent bleed-over into other tests
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    // --- Shared helpers for building presenters and awaiting state ---
    private fun makeOffer(
        id: String,
        isMy: Boolean,
        quoteMethods: List<String>,
        baseMethods: List<String>,
        makerId: String = id,
    ): OfferItemPresentationModel {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L)
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) })
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = makerId, hash = makerId, id = makerId),
            )
        val offer =
            BisqEasyOfferVO(
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
                supportedLanguageCodes = listOf("en"),
            )
        val user: UserProfileVO = createMockUserProfile("maker-$makerId")
        val reputation = ReputationScoreVO(0, 0.0, 0)
        val dto =
            OfferItemPresentationDto(
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
                reputationScore = reputation,
            )
        return OfferItemPresentationModel(dto)
    }

    private fun buildPresenterWithOffers(allOffers: List<OfferItemPresentationModel>): OfferbookPresenter {
        // --- Mocks and fakes for MainPresenter ---
        val tradesServiceFacade = mockk<TradesServiceFacade>()
        every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())
        val userProfileServiceForMain = FakeUserProfileServiceFacade()
        val openTradesNotificationService = mockk<OpenTradesNotificationService>(relaxed = true)
        val settingsService = FakeSettingsServiceFacade()
        val tradeReadStateRepository = FakeTradeReadStateRepository()
        val urlLauncher = mockk<UrlLauncher>(relaxed = true)
        val mainPresenter =
            MainPresenter(
                tradesServiceFacade,
                userProfileServiceForMain,
                openTradesNotificationService,
                settingsService,
                tradeReadStateRepository,
                urlLauncher,
                TestApplicationLifecycleService(),
            )
        // --- Dependencies for OfferbookPresenter ---
        val offersFlow = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
        val marketFlow =
            MutableStateFlow(
                OfferbookMarket(
                    MarketVO(
                        baseCurrencyCode = "BTC",
                        quoteCurrencyCode = "USD",
                        baseCurrencyName = "Bitcoin",
                        quoteCurrencyName = "US Dollar",
                    ),
                ),
            )
        val offersService = mockk<OffersServiceFacade>()
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns marketFlow
        coEvery { offersService.deleteOffer(any()) } returns Result.success(true)
        // User profile facade for OfferbookPresenter
        val offerUserProfileService = mockk<UserProfileServiceFacade>(relaxed = true)
        val me = createMockUserProfile("me")
        every { offerUserProfileService.selectedUserProfile } returns MutableStateFlow(me)
        coEvery { offerUserProfileService.isUserIgnored(any()) } returns false
        coEvery { offerUserProfileService.getUserProfileIcon(any(), any()) } returns mockk(relaxed = true)
        coEvery { offerUserProfileService.getUserProfileIcon(any()) } returns mockk(relaxed = true)
        // Market price and reputation services (not exercised in these SELL-offer tests)
        val marketPriceServiceFacade =
            object : MarketPriceServiceFacade(mockk(relaxed = true)) {
                override fun findMarketPriceItem(marketVO: MarketVO) = null

                override fun findUSDMarketPriceItem() = null

                override fun refreshSelectedFormattedMarketPrice() {}

                override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
            }
        val reputationService = mockk<ReputationServiceFacade>(relaxed = true)
        val takeOfferPresenter = mockk<TakeOfferPresenter>(relaxed = true)
        val createOfferPresenter = mockk<CreateOfferPresenter>(relaxed = true)
        val presenter =
            OfferbookPresenter(
                mainPresenter,
                offersService,
                takeOfferPresenter,
                createOfferPresenter,
                marketPriceServiceFacade,
                offerUserProfileService,
                reputationService,
            )
        offersFlow.value = allOffers
        presenter.onViewAttached()
        return presenter
    }

    private suspend fun awaitBaseline(
        presenter: OfferbookPresenter,
        expectedPay: Set<String>,
        expectedSettle: Set<String>,
    ) {
        combine(
            presenter.availablePaymentMethodIds,
            presenter.availableSettlementMethodIds,
        ) { pay, settle -> pay to settle }
            .filter { (pay, settle) -> pay == expectedPay && settle == expectedSettle }
            .first()
    }

    private suspend fun awaitSortedCount(
        presenter: OfferbookPresenter,
        expected: Int,
    ) {
        presenter.sortedFilteredOffers
            .filter { it.size == expected }
            .first()
    }

    @Test
    fun test_onlyMyOffers_filters_to_user_offers() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer("o1", isMy = true, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                    makeOffer(
                        "o3",
                        isMy = false,
                        quoteMethods = listOf("CASH_APP"),
                        baseMethods = listOf("MAIN_CHAIN"),
                    ),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)

            presenter.setSelectedPaymentMethodIds(expectedPayments)
            presenter.setSelectedSettlementMethodIds(expectedSettlements)
            awaitSortedCount(presenter, allOffers.size)

            presenter.setOnlyMyOffers(true)
            awaitSortedCount(presenter, 1)
            assertEquals(1, presenter.sortedFilteredOffers.value.size)
            assertTrue(presenter.sortedFilteredOffers.value.all { it.isMyOffer })
        }

    @Test
    fun test_empty_selection_shows_no_offers() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer("o1", isMy = true, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                    makeOffer(
                        "o3",
                        isMy = false,
                        quoteMethods = listOf("CASH_APP"),
                        baseMethods = listOf("MAIN_CHAIN"),
                    ),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)

            presenter.setSelectedPaymentMethodIds(expectedPayments)
            presenter.setSelectedSettlementMethodIds(expectedSettlements)
            awaitSortedCount(presenter, allOffers.size)

            presenter.setSelectedPaymentMethodIds(emptySet())
            awaitSortedCount(presenter, 0)
            assertEquals(0, presenter.sortedFilteredOffers.value.size)
            assertEquals(expectedPayments, presenter.availablePaymentMethodIds.value)

            presenter.setSelectedSettlementMethodIds(emptySet())
            awaitSortedCount(presenter, 0)
            assertEquals(0, presenter.sortedFilteredOffers.value.size)
            assertEquals(expectedSettlements, presenter.availableSettlementMethodIds.value)
        }

    @Test
    fun test_baseline_availability_remains_stable() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer("o1", isMy = true, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                    makeOffer(
                        "o3",
                        isMy = false,
                        quoteMethods = listOf("CASH_APP"),
                        baseMethods = listOf("MAIN_CHAIN"),
                    ),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)
            assertEquals(expectedPayments, presenter.availablePaymentMethodIds.value)
            assertEquals(expectedSettlements, presenter.availableSettlementMethodIds.value)

            presenter.setSelectedPaymentMethodIds(emptySet())
            awaitSortedCount(presenter, 0)
            assertEquals(expectedPayments, presenter.availablePaymentMethodIds.value)

            presenter.setSelectedSettlementMethodIds(emptySet())
            awaitSortedCount(presenter, 0)
            assertEquals(expectedSettlements, presenter.availableSettlementMethodIds.value)
        }

    @Test
    @Ignore("Flaky on CI/Linux; temporarily disabled until Offerbook filter timing is stabilized")
    fun test_selection_restoration_shows_all_offers() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer("o1", isMy = true, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                    makeOffer(
                        "o3",
                        isMy = false,
                        quoteMethods = listOf("CASH_APP"),
                        baseMethods = listOf("MAIN_CHAIN"),
                    ),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)

            // Clear selections, then restore
            presenter.setSelectedPaymentMethodIds(emptySet())
            presenter.setSelectedSettlementMethodIds(emptySet())
            awaitSortedCount(presenter, 0)

            presenter.setSelectedPaymentMethodIds(expectedPayments)
            presenter.setSelectedSettlementMethodIds(expectedSettlements)
            awaitSortedCount(presenter, allOffers.size)
            assertEquals(allOffers.size, presenter.sortedFilteredOffers.value.size)
        }

    @Test
    @Ignore("Flaky on CI/Linux; temporarily disabled until Offerbook filter timing is stabilized")
    fun test_onlyMyOffers_with_no_own_offers_marks_filters_active() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer("o1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)

            // Start from a state where all methods are selected and all offers are visible
            presenter.setSelectedPaymentMethodIds(expectedPayments)
            presenter.setSelectedSettlementMethodIds(expectedSettlements)
            awaitSortedCount(presenter, allOffers.size)

            // Enabling "Only my offers" when user has no offers should yield an empty list
            // but mark filters as active so the controller remains visible on the screen.
            presenter.setOnlyMyOffers(true)
            awaitSortedCount(presenter, 0)
            // Let filter UI state and its upstream flows fully settle, then assert on the
            // latest snapshot instead of waiting on a filtered flow.
            advanceUntilIdle()
            val filterState = presenter.filterUiState.value
            assertTrue(filterState.onlyMyOffers)
            assertTrue(filterState.hasActiveFilters)
        }

    @Test
    @Ignore("Flaky on CI/Linux; temporarily disabled until Offerbook filter timing is stabilized")
    fun test_method_filters_mark_filters_active_when_list_empty() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer("o1", isMy = true, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)

            // Clear all selections via the presenter API; because this is treated as a manual
            // filter, it should hide all offers but mark filters as active.
            presenter.setSelectedPaymentMethodIds(emptySet())
            presenter.setSelectedSettlementMethodIds(emptySet())
            awaitSortedCount(presenter, 0)

            // Let filter UI state and its upstream flows fully settle, then assert on the
            // latest snapshot instead of waiting on a filtered flow.
            advanceUntilIdle()
            val filterState = presenter.filterUiState.value
            assertTrue(
                filterState.payment.any { icon -> !icon.selected } ||
                    filterState.settlement.any { icon -> !icon.selected },
            )
            assertTrue(filterState.hasActiveFilters)
        }

    @Test
    fun test_onlyMyOffers_respects_method_filters() =
        runTest(testDispatcher) {
            val allOffers =
                listOf(
                    makeOffer(
                        "m1",
                        isMy = true,
                        quoteMethods = listOf("NATIONAL_BANK"),
                        baseMethods = listOf("MAIN_CHAIN"),
                    ),
                    makeOffer("m2", isMy = true, quoteMethods = listOf("WISE"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("o3", isMy = false, quoteMethods = listOf("WISE"), baseMethods = listOf("LIGHTNING")),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            val expectedPayments = allOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val expectedSettlements = allOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, expectedPayments, expectedSettlements)

            presenter.setSelectedPaymentMethodIds(expectedPayments)
            presenter.setSelectedSettlementMethodIds(expectedSettlements)
            awaitSortedCount(presenter, allOffers.size)

            presenter.setOnlyMyOffers(true)
            awaitSortedCount(presenter, 2)
            assertTrue(presenter.sortedFilteredOffers.value.all { it.isMyOffer })

            // Unselect WISE -> should keep only NATIONAL_BANK my offer
            presenter.setSelectedPaymentMethodIds(setOf("NATIONAL_BANK"))
            awaitSortedCount(presenter, 1)
            assertEquals(1, presenter.sortedFilteredOffers.value.size)
            assertTrue(
                presenter.sortedFilteredOffers.value
                    .first()
                    .quoteSidePaymentMethods
                    .contains("NATIONAL_BANK"),
            )
        }

    @Test
    fun test_cross_market_payment_filter_persistence() =
        runTest(testDispatcher) {
            // Scenario: User filters to only WISE and REVOLUT,
            // then the available payment methods change (simulating market switch).
            // Expected: Offers with WISE or REVOLUT should still be visible.
            // Bug: All offers get filtered out because selected methods become empty
            // when availability changes, but hasManualPaymentFilter stays true.

            // Initial offers: WISE, REVOLUT, SEPA
            val initialOffers =
                listOf(
                    makeOffer("a1", isMy = false, quoteMethods = listOf("WISE"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("a2", isMy = false, quoteMethods = listOf("REVOLUT"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("a3", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                )

            // Build presenter with mutable offers flow so we can simulate market changes
            val tradesServiceFacade = mockk<TradesServiceFacade>()
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())
            val userProfileServiceForMain = FakeUserProfileServiceFacade()
            val openTradesNotificationService = mockk<OpenTradesNotificationService>(relaxed = true)
            val settingsService = FakeSettingsServiceFacade()
            val tradeReadStateRepository = FakeTradeReadStateRepository()
            val urlLauncher = mockk<UrlLauncher>(relaxed = true)
            val mainPresenter =
                MainPresenter(
                    tradesServiceFacade,
                    userProfileServiceForMain,
                    openTradesNotificationService,
                    settingsService,
                    tradeReadStateRepository,
                    urlLauncher,
                    TestApplicationLifecycleService(),
                )

            val offersFlow = MutableStateFlow<List<OfferItemPresentationModel>>(initialOffers)
            val marketFlow =
                MutableStateFlow(
                    OfferbookMarket(
                        MarketVO(
                            baseCurrencyCode = "BTC",
                            quoteCurrencyCode = "USD",
                            baseCurrencyName = "Bitcoin",
                            quoteCurrencyName = "US Dollar",
                        ),
                    ),
                )
            val offersService = mockk<OffersServiceFacade>()
            every { offersService.offerbookListItems } returns offersFlow
            every { offersService.selectedOfferbookMarket } returns marketFlow
            coEvery { offersService.deleteOffer(any()) } returns Result.success(true)

            val offerUserProfileService = mockk<UserProfileServiceFacade>(relaxed = true)
            val me = createMockUserProfile("me")
            every { offerUserProfileService.selectedUserProfile } returns MutableStateFlow(me)
            coEvery { offerUserProfileService.isUserIgnored(any()) } returns false
            coEvery { offerUserProfileService.getUserProfileIcon(any(), any()) } returns mockk(relaxed = true)
            coEvery { offerUserProfileService.getUserProfileIcon(any()) } returns mockk(relaxed = true)

            val marketPriceServiceFacade =
                object : MarketPriceServiceFacade(mockk(relaxed = true)) {
                    override fun findMarketPriceItem(marketVO: MarketVO) = null

                    override fun findUSDMarketPriceItem() = null

                    override fun refreshSelectedFormattedMarketPrice() {}

                    override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
                }
            val reputationService = mockk<ReputationServiceFacade>(relaxed = true)
            val takeOfferPresenter = mockk<TakeOfferPresenter>(relaxed = true)
            val createOfferPresenter = mockk<CreateOfferPresenter>(relaxed = true)

            val presenter =
                OfferbookPresenter(
                    mainPresenter,
                    offersService,
                    takeOfferPresenter,
                    createOfferPresenter,
                    marketPriceServiceFacade,
                    offerUserProfileService,
                    reputationService,
                )
            presenter.onViewAttached()
            runCurrent()

            // Wait for baseline availability
            val initialPayments = initialOffers.flatMap { it.quoteSidePaymentMethods }.toSet()
            val initialSettlements = initialOffers.flatMap { it.baseSidePaymentMethods }.toSet()
            awaitBaseline(presenter, initialPayments, initialSettlements)

            // User manually filters to only WISE and REVOLUT
            presenter.setSelectedPaymentMethodIds(setOf("WISE", "REVOLUT"))
            presenter.setSelectedSettlementMethodIds(initialSettlements)
            awaitSortedCount(presenter, 2) // Should show 2 offers (WISE and REVOLUT)

            // Now simulate market change: new offers with WISE, REVOLUT, and NATIONAL_BANK
            val newOffers =
                listOf(
                    makeOffer("b1", isMy = false, quoteMethods = listOf("WISE"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("b2", isMy = false, quoteMethods = listOf("REVOLUT"), baseMethods = listOf("LIGHTNING")),
                    makeOffer("b3", isMy = false, quoteMethods = listOf("NATIONAL_BANK"), baseMethods = listOf("MAIN_CHAIN")),
                )
            offersFlow.value = newOffers
            runCurrent()
            advanceUntilIdle()

            // BUG: At this point, the selected payment methods should still include WISE and REVOLUT
            // and we should see 2 offers (b1 and b2), but due to the bug, all offers are filtered out
            val visibleOffers = presenter.sortedFilteredOffers.value

            // Expected: 2 offers (WISE and REVOLUT)
            // Actual (with bug): 0 offers
            assertEquals(2, visibleOffers.size, "Should show offers with WISE or REVOLUT payment methods")
            assertTrue(
                visibleOffers.all { offer ->
                    offer.quoteSidePaymentMethods.any { it in setOf("WISE", "REVOLUT") }
                },
                "All visible offers should have WISE or REVOLUT as payment method",
            )
        }

    // --- Minimal helpers/types for tests ---

    private class TestCoroutineJobsManager(
        private val dispatcher: CoroutineDispatcher,
        override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
    ) : CoroutineJobsManager {
        private val scope = CoroutineScope(dispatcher + SupervisorJob())
        private val ioScope = CoroutineScope(dispatcher + SupervisorJob())
        private val jobs = mutableSetOf<Job>()

        override suspend fun dispose() {
            scope.cancel()
            ioScope.cancel()
            jobs.clear()
        }

        override fun getScope(): CoroutineScope = scope
    }

    private class NoopNavigationManager : NavigationManager {
        private val _currentTab = MutableStateFlow<TabNavRoute?>(null)
        override val currentTab: StateFlow<TabNavRoute?> get() = _currentTab.asStateFlow()

        override fun setRootNavController(navController: NavHostController?) {}

        override fun setTabNavController(navController: NavHostController?) {}

        override fun isAtMainScreen(): Boolean = true

        override fun isAtHomeTab(): Boolean = true

        override fun showBackButton(): Boolean = false

        override fun navigate(
            destination: NavRoute,
            customSetup: (NavOptionsBuilder) -> Unit,
            onCompleted: (() -> Unit)?,
        ) {
            onCompleted?.invoke()
        }

        override fun navigateToTab(
            destination: TabNavRoute,
            saveStateOnPopUp: Boolean,
            shouldLaunchSingleTop: Boolean,
            shouldRestoreState: Boolean,
        ) {
            _currentTab.value = destination
        }

        override fun navigateBackTo(
            destination: NavRoute,
            shouldInclusive: Boolean,
            shouldSaveState: Boolean,
        ) {
        }

        override fun navigateFromUri(uri: String) {}

        override fun navigateBack(onCompleted: (() -> Unit)?) {
            onCompleted?.invoke()
        }
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

    private class FakeTradeReadStateRepository : TradeReadStateRepository {
        override val data: Flow<TradeReadStateMap> = flowOf(TradeReadStateMap())

        override suspend fun setCount(
            tradeId: String,
            count: Int,
        ) {
        }

        override suspend fun clearId(tradeId: String) {}
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
}
