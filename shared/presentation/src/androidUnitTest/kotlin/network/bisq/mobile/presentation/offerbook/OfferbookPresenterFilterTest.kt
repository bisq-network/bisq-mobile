package network.bisq.mobile.presentation.offerbook

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.OfferbookFilterConfigRepository
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterFilterTest : PlatformPresentationKoinTestBase() {
    // --- Shared helpers for building presenters and awaiting state ---
    private class FakeOfferbookFilterConfigRepository(
        initialConfigs: OfferbookFilterConfigs = OfferbookFilterConfigs(),
    ) : OfferbookFilterConfigRepository {
        private val _data = MutableStateFlow(initialConfigs)
        override val data: StateFlow<OfferbookFilterConfigs> = _data

        override suspend fun getConfig(marketKey: String): OfferbookFilterConfig = _data.value.configsByMarket[marketKey] ?: OfferbookFilterConfig()

        override suspend fun setConfig(
            marketKey: String,
            config: OfferbookFilterConfig,
        ) {
            _data.value = _data.value.copy(configsByMarket = _data.value.configsByMarket + (marketKey to config))
        }
    }

    private fun makeOffer(
        id: String,
        isMy: Boolean,
        quoteMethods: List<String>,
        baseMethods: List<String>,
        makerId: String = id,
        direction: DirectionEnum = DirectionEnum.SELL,
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
                direction = direction, // filter matches the MIRRORED (taker's) direction
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

    private fun buildPresenterWithOffers(
        allOffers: List<OfferItemPresentationModel>,
        reputationService: ReputationServiceFacade = mockk(relaxed = true),
    ): OfferbookPresenter {
        // --- Mocks and fakes for MainPresenter ---
        val mainPresenter =
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())
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
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
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
        val takeOfferCoordinator = mockk<TakeOfferCoordinator>(relaxed = true)
        val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)
        val presenter =
            OfferbookPresenter(
                mainPresenter,
                offersService,
                takeOfferCoordinator,
                createOfferCoordinator,
                marketPriceServiceFacade,
                offerUserProfileService,
                reputationService,
                tradeRestrictingAlertServiceFacade,
                FakeOfferbookFilterConfigRepository(),
                configServiceFacade = FakeConfigServiceFacade(),
                appUpdateLinker = FakeAppUpdateLinker(),
                computationDispatcher = testDispatcher,
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
        runTest {
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
        runTest {
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
        runTest {
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
    fun test_selection_restoration_shows_all_offers() =
        runTest {
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
    fun test_onlyMyOffers_with_no_own_offers_marks_filters_active() =
        runTest {
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
    fun test_method_filters_mark_filters_active_when_list_empty() =
        runTest {
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
        runTest {
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
    fun test_restores_initial_market_filter_config_from_repository_and_persists_changes() =
        runTest {
            val initialOffers =
                listOf(
                    makeOffer("wise", isMy = false, quoteMethods = listOf("WISE"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("sepa", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                )
            val repository =
                FakeOfferbookFilterConfigRepository(
                    OfferbookFilterConfigs(
                        mapOf(
                            "BTC/USD" to
                                OfferbookFilterConfig(
                                    selectedPaymentMethodIds = setOf("WISE"),
                                    selectedSettlementMethodIds = setOf("MAIN_CHAIN"),
                                    onlyMyOffers = false,
                                    hasManualPaymentFilter = true,
                                    hasManualSettlementFilter = false,
                                ),
                        ),
                    ),
                )
            val mainPresenter = MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())
            val offersFlow = MutableStateFlow(initialOffers)
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
            every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
            coEvery { offersService.deleteOffer(any()) } returns Result.success(true)
            val offerUserProfileService = mockk<UserProfileServiceFacade>(relaxed = true)
            every { offerUserProfileService.selectedUserProfile } returns MutableStateFlow(createMockUserProfile("me"))
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
            val presenter =
                OfferbookPresenter(
                    mainPresenter,
                    offersService,
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    marketPriceServiceFacade,
                    offerUserProfileService,
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                    repository,
                    configServiceFacade = FakeConfigServiceFacade(),
                    appUpdateLinker = FakeAppUpdateLinker(),
                    computationDispatcher = testDispatcher,
                )

            presenter.onViewAttached()
            advanceUntilIdle()

            awaitSortedCount(presenter, 1)
            assertEquals(
                setOf("WISE"),
                presenter.filterUiState.value.payment
                    .filter { it.selected }
                    .map { it.id }
                    .toSet(),
            )

            presenter.setOnlyMyOffers(true)
            advanceUntilIdle()

            assertEquals(true, repository.getConfig("BTC/USD").onlyMyOffers)
            assertEquals(setOf("WISE"), repository.getConfig("BTC/USD").selectedPaymentMethodIds)
        }

    @Test
    fun test_cross_market_payment_filter_persistence() =
        runTest {
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
            val mainPresenter =
                MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())

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
            every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
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
            val takeOfferCoordinator = mockk<TakeOfferCoordinator>(relaxed = true)
            val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)
            val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)

            val presenter =
                OfferbookPresenter(
                    mainPresenter,
                    offersService,
                    takeOfferCoordinator,
                    createOfferCoordinator,
                    marketPriceServiceFacade,
                    offerUserProfileService,
                    reputationService,
                    tradeRestrictingAlertServiceFacade,
                    FakeOfferbookFilterConfigRepository(),
                    configServiceFacade = FakeConfigServiceFacade(),
                    appUpdateLinker = FakeAppUpdateLinker(),
                    computationDispatcher = testDispatcher,
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

    /**
     * Regression for the "offers appear seconds late" field report: every BUY-direction offer needs
     * MY reputation score (I would be the seller), and the pipeline used to fetch it once PER OFFER —
     * sequential network round trips on the client (debug builds bypass the local reputation cache),
     * blocking the list emission for seconds although the offers were already cached. The score must
     * be fetched at most once per pipeline run and memoized across runs.
     */
    @Test
    fun test_my_reputation_fetched_once_for_buy_offers_and_memoized_across_pipeline_runs() =
        runTest {
            val reputationService =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } returns Result.success(ReputationScoreVO(Long.MAX_VALUE, 5.0, 1))
                }
            // direction = BUY -> maker buys -> I would sell -> shown on the SELL tab, needs MY score
            val allOffers =
                listOf(
                    makeOffer("b1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN"), direction = DirectionEnum.BUY),
                    makeOffer("b2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING"), direction = DirectionEnum.BUY),
                    makeOffer("b3", isMy = false, quoteMethods = listOf("WISE"), baseMethods = listOf("MAIN_CHAIN"), direction = DirectionEnum.BUY),
                )
            val presenter = buildPresenterWithOffers(allOffers, reputationService)
            runCurrent()

            presenter.onSelectDirection(DirectionEnum.SELL)
            awaitSortedCount(presenter, allOffers.size)

            coVerify(exactly = 1) { reputationService.getReputation(any()) }

            // A further pipeline run (direction toggled away and back) must reuse the memoized score
            presenter.onSelectDirection(DirectionEnum.BUY)
            awaitSortedCount(presenter, 0)
            presenter.onSelectDirection(DirectionEnum.SELL)
            awaitSortedCount(presenter, allOffers.size)

            coVerify(exactly = 1) { reputationService.getReputation(any()) }
        }

    /**
     * Direction-aware empty state: landing on a tab with 0 offers while the other tab has matches
     * must expose the other tab's count so the UI can offer the switch instead of a bare
     * "no offers" that reads as broken against the market list's advertised count.
     */
    @Test
    fun test_opposite_direction_count_exposed_when_selected_tab_is_empty() =
        runTest {
            // direction = BUY -> mirror SELL -> these show on the SELL tab; default tab is BUY
            val allOffers =
                listOf(
                    makeOffer("b1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN"), direction = DirectionEnum.BUY),
                    makeOffer("b2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING"), direction = DirectionEnum.BUY),
                )
            val presenter = buildPresenterWithOffers(allOffers)
            runCurrent()

            awaitSortedCount(presenter, 0)
            presenter.oppositeDirectionOffersCount
                .filter { it == allOffers.size }
                .first()

            // Switching to the tab that has the offers empties the opposite count again
            presenter.onSelectDirection(DirectionEnum.SELL)
            awaitSortedCount(presenter, allOffers.size)
            presenter.oppositeDirectionOffersCount
                .filter { it == 0 }
                .first()
        }

    /**
     * Regression for "all markets empty": the reputation lookup runs inside the mapLatest collector,
     * so a facade that THROWS (not just returns a failure Result — e.g. a network-layer exception on
     * device) must not kill the filtering pipeline. Offers must still display, and the throw must
     * not be memoized so a later run can recover.
     */
    @Test
    fun test_throwing_reputation_lookup_does_not_kill_the_pipeline() =
        runTest {
            val reputationService =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } throws RuntimeException("network layer blew up")
                }
            val allOffers =
                listOf(
                    makeOffer("b1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN"), direction = DirectionEnum.BUY),
                    makeOffer("b2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING"), direction = DirectionEnum.BUY),
                )
            val presenter = buildPresenterWithOffers(allOffers, reputationService)
            runCurrent()

            presenter.onSelectDirection(DirectionEnum.SELL)
            // The pipeline must survive the throw and still emit the offers
            awaitSortedCount(presenter, allOffers.size)

            // And a later pipeline run must also survive (the throw is not memoized)
            presenter.onSelectDirection(DirectionEnum.BUY)
            awaitSortedCount(presenter, 0)
            presenter.onSelectDirection(DirectionEnum.SELL)
            awaitSortedCount(presenter, allOffers.size)
        }

    /**
     * Control: SELL-direction offers (the maker is the seller) never need my score — the only fetch
     * is the single attach-time warmup, no matter how many pipeline runs happen.
     */
    @Test
    fun test_pipeline_never_fetches_reputation_for_sell_direction_offers() =
        runTest {
            val reputationService = mockk<ReputationServiceFacade>(relaxed = true)
            val allOffers =
                listOf(
                    makeOffer("s1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
                    makeOffer("s2", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("LIGHTNING")),
                )
            val presenter = buildPresenterWithOffers(allOffers, reputationService)
            runCurrent()

            awaitSortedCount(presenter, allOffers.size)
            presenter.onSelectDirection(DirectionEnum.SELL)
            awaitSortedCount(presenter, 0)
            presenter.onSelectDirection(DirectionEnum.BUY)
            awaitSortedCount(presenter, allOffers.size)

            coVerify(exactly = 1) { reputationService.getReputation(any()) }
        }

    /**
     * The attach-time warmup pre-fetches my score so the first pipeline run over BUY-direction
     * offers never pays a network round trip while the UI sits on stale state.
     */
    @Test
    fun test_my_reputation_warmed_up_at_attach() =
        runTest {
            val reputationService = mockk<ReputationServiceFacade>(relaxed = true)
            val presenter = buildPresenterWithOffers(emptyList(), reputationService)
            advanceUntilIdle()

            coVerify(exactly = 1) { reputationService.getReputation(any()) }
            // Keep the presenter referenced so the pipeline stays alive for the duration
            awaitSortedCount(presenter, 0)
        }

    /**
     * Race regression: the attach-time warmup and the first pipeline run over BUY-direction offers
     * can look up the score concurrently on a cold cache. The lookups must coalesce — the second
     * caller waits for the in-flight fetch and reuses it — never issue a duplicate network request.
     */
    @Test
    fun test_concurrent_warmup_and_pipeline_lookups_coalesce_into_one_fetch() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val reputationService =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } coAnswers {
                        gate.await()
                        Result.success(ReputationScoreVO(Long.MAX_VALUE, 5.0, 1))
                    }
                }
            val allOffers =
                listOf(
                    makeOffer("b1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN"), direction = DirectionEnum.BUY),
                )
            val presenter = buildPresenterWithOffers(allOffers, reputationService)
            runCurrent() // warmup starts and parks on the gated fetch

            // Pipeline run needs the same score while the warmup fetch is still in flight
            presenter.onSelectDirection(DirectionEnum.SELL)
            runCurrent()

            gate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { reputationService.getReputation(any()) }
            assertEquals(allOffers.size, presenter.sortedFilteredOffers.value.size)
        }

    /**
     * While the pipeline recomputes for changed inputs, [OfferbookPresenter.isRefilteringOffers]
     * must be true so the UI can show progress instead of the PREVIOUS run's stale (empty) state —
     * the "empty state flashes with a mislabeled switch hint for 1-2s" field report.
     */
    @Test
    fun test_refiltering_state_exposed_while_pipeline_run_is_in_flight() =
        runTest {
            val reputationService =
                mockk<ReputationServiceFacade> {
                    coEvery { getReputation(any()) } coAnswers {
                        delay(500)
                        Result.success(ReputationScoreVO(Long.MAX_VALUE, 5.0, 1))
                    }
                }
            val allOffers =
                listOf(
                    makeOffer("b1", isMy = false, quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN"), direction = DirectionEnum.BUY),
                )
            val presenter = buildPresenterWithOffers(allOffers, reputationService)
            runCurrent()

            // Switch to the tab whose run pays the (slow) fetch: the recompute is in flight
            presenter.onSelectDirection(DirectionEnum.SELL)
            runCurrent()
            assertTrue(presenter.isRefilteringOffers.value, "recompute must be flagged while in flight")

            // Once the run lands the flag clears and the offers are published
            advanceUntilIdle()
            assertEquals(false, presenter.isRefilteringOffers.value)
            assertEquals(allOffers.size, presenter.sortedFilteredOffers.value.size)
        }
}
