package network.bisq.mobile.presentation.offerbook

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.OfferbookFilterConfigRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.di.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
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
class OfferbookPresenterFilterPersistenceTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    private class FakeOfferbookFilterConfigRepository(
        initialConfigs: OfferbookFilterConfigs = OfferbookFilterConfigs(),
        private val getConfigFailure: Throwable? = null,
        private val getConfigDelayMillis: Long = 0,
    ) : OfferbookFilterConfigRepository {
        private val _data = MutableStateFlow(initialConfigs)
        override val data: StateFlow<OfferbookFilterConfigs> = _data.asStateFlow()

        override suspend fun getConfig(marketKey: String): OfferbookFilterConfig {
            if (getConfigDelayMillis > 0) {
                delay(getConfigDelayMillis)
            }
            getConfigFailure?.let { throw it }
            return _data.value.configsByMarket[marketKey] ?: OfferbookFilterConfig()
        }

        override suspend fun setConfig(
            marketKey: String,
            config: OfferbookFilterConfig,
        ) {
            _data.value = _data.value.copy(configsByMarket = _data.value.configsByMarket + (marketKey to config))
        }
    }

    private data class PresenterFixture(
        val presenter: OfferbookPresenter,
        val repository: FakeOfferbookFilterConfigRepository,
        val offersFlow: MutableStateFlow<List<OfferItemPresentationModel>>,
        val marketFlow: MutableStateFlow<OfferbookMarket>,
    )

    private fun createFixture(
        initialConfigs: OfferbookFilterConfigs = OfferbookFilterConfigs(),
        initialMarket: MarketVO = market("USD"),
        initialOffers: List<OfferItemPresentationModel> = defaultOffers(),
        getConfigFailure: Throwable? = null,
        getConfigDelayMillis: Long = 0,
    ): PresenterFixture {
        val mainPresenter = MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())
        val repository = FakeOfferbookFilterConfigRepository(initialConfigs, getConfigFailure, getConfigDelayMillis)
        val offersFlow = MutableStateFlow(initialOffers)
        val marketFlow = MutableStateFlow(OfferbookMarket(initialMarket))
        val offersService = mockk<OffersServiceFacade>()
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns marketFlow
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
        coEvery { offersService.deleteOffer(any()) } returns Result.success(true)

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(createMockUserProfile("me"))
        coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false
        coEvery { userProfileServiceFacade.getUserProfileIcon(any(), any()) } returns mockk(relaxed = true)
        coEvery { userProfileServiceFacade.getUserProfileIcon(any()) } returns mockk(relaxed = true)

        val presenter =
            OfferbookPresenter(
                mainPresenter = mainPresenter,
                offersServiceFacade = offersService,
                takeOfferCoordinator = mockk<TakeOfferCoordinator>(relaxed = true),
                createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true),
                marketPriceServiceFacade = testMarketPriceServiceFacade(),
                userProfileServiceFacade = userProfileServiceFacade,
                reputationServiceFacade = mockk<ReputationServiceFacade>(relaxed = true),
                tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true),
                offerbookFilterConfigRepository = repository,
                configServiceFacade = FakeConfigServiceFacade(),
                computationDispatcher = testDispatcher,
            )
        presenter.onViewAttached()
        return PresenterFixture(presenter, repository, offersFlow, marketFlow)
    }

    private suspend fun awaitBaseline(
        presenter: OfferbookPresenter,
        expectedPaymentMethodIds: Set<String> = setOf("SEPA", "WISE"),
        expectedSettlementMethodIds: Set<String> = setOf("MAIN_CHAIN", "LIGHTNING"),
    ) {
        combine(
            presenter.availablePaymentMethodIds,
            presenter.availableSettlementMethodIds,
        ) { paymentMethodIds, settlementMethodIds -> paymentMethodIds to settlementMethodIds }
            .filter { (paymentMethodIds, settlementMethodIds) ->
                paymentMethodIds == expectedPaymentMethodIds && settlementMethodIds == expectedSettlementMethodIds
            }.first()
    }

    private suspend fun awaitSelected(
        presenter: OfferbookPresenter,
        expectedPaymentMethodIds: Set<String>,
        expectedSettlementMethodIds: Set<String>,
    ) {
        combine(
            presenter.selectedPaymentMethodIds,
            presenter.selectedSettlementMethodIds,
        ) { paymentMethodIds, settlementMethodIds -> paymentMethodIds to settlementMethodIds }
            .filter { (paymentMethodIds, settlementMethodIds) ->
                paymentMethodIds == expectedPaymentMethodIds && settlementMethodIds == expectedSettlementMethodIds
            }.first()
    }

    @Test
    fun `when initial filter config restore fails then defaults are used and presenter continues`() =
        runTest(testDispatcher) {
            val fixture = createFixture(getConfigFailure = IllegalStateException("Cannot read filters"))
            try {
                awaitBaseline(fixture.presenter)
                awaitSelected(fixture.presenter, setOf("SEPA", "WISE"), setOf("MAIN_CHAIN", "LIGHTNING"))
                advanceUntilIdle()

                assertEquals(setOf("SEPA", "WISE"), fixture.presenter.selectedPaymentMethodIds.value)
                assertEquals(setOf("MAIN_CHAIN", "LIGHTNING"), fixture.presenter.selectedSettlementMethodIds.value)
                assertFalse(fixture.presenter.onlyMyOffers.value)
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    @Test
    fun `when initial restore is slow then restored manual selections are not auto-overwritten`() =
        runTest(testDispatcher) {
            val usdConfig =
                OfferbookFilterConfig(
                    selectedPaymentMethodIds = setOf("SEPA"),
                    selectedSettlementMethodIds = setOf("LIGHTNING"),
                    hasManualPaymentFilter = true,
                    hasManualSettlementFilter = true,
                )
            val fixture =
                createFixture(
                    initialConfigs = OfferbookFilterConfigs(mapOf("BTC/USD" to usdConfig)),
                    getConfigDelayMillis = 1_000,
                )
            try {
                advanceUntilIdle()

                assertEquals(setOf("SEPA"), fixture.presenter.selectedPaymentMethodIds.value)
                assertEquals(setOf("LIGHTNING"), fixture.presenter.selectedSettlementMethodIds.value)
                assertEquals(usdConfig, fixture.repository.getConfig("BTC/USD"))
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    @Test
    fun `when selected market changes then current config is persisted and next market config is restored`() =
        runTest(testDispatcher) {
            val eurConfig =
                OfferbookFilterConfig(
                    selectedPaymentMethodIds = setOf("SEPA"),
                    selectedSettlementMethodIds = setOf("LIGHTNING"),
                    onlyMyOffers = true,
                    hasManualPaymentFilter = true,
                    hasManualSettlementFilter = true,
                )
            val fixture =
                createFixture(
                    initialConfigs = OfferbookFilterConfigs(mapOf("BTC/EUR" to eurConfig)),
                )
            try {
                awaitBaseline(fixture.presenter)

                fixture.presenter.setSelectedPaymentMethodIds(setOf("WISE"))
                fixture.presenter.setSelectedSettlementMethodIds(setOf("MAIN_CHAIN"))
                awaitSelected(fixture.presenter, setOf("WISE"), setOf("MAIN_CHAIN"))
                advanceUntilIdle()

                fixture.marketFlow.value = OfferbookMarket(market("EUR"))
                awaitSelected(fixture.presenter, setOf("SEPA"), setOf("LIGHTNING"))
                advanceUntilIdle()

                val savedUsdConfig = fixture.repository.getConfig("BTC/USD")
                assertEquals(setOf("WISE"), savedUsdConfig.selectedPaymentMethodIds)
                assertEquals(setOf("MAIN_CHAIN"), savedUsdConfig.selectedSettlementMethodIds)
                assertEquals(setOf("SEPA"), fixture.presenter.selectedPaymentMethodIds.value)
                assertEquals(setOf("LIGHTNING"), fixture.presenter.selectedSettlementMethodIds.value)
                assertTrue(fixture.presenter.onlyMyOffers.value)
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    @Test
    fun `when available payment methods are auto-selected then config is persisted for current market`() =
        runTest(testDispatcher) {
            val fixture = createFixture()
            try {
                awaitBaseline(fixture.presenter)
                awaitSelected(fixture.presenter, setOf("SEPA", "WISE"), setOf("MAIN_CHAIN", "LIGHTNING"))
                advanceUntilIdle()

                val savedConfig = fixture.repository.getConfig("BTC/USD")
                assertEquals(setOf("SEPA", "WISE"), savedConfig.selectedPaymentMethodIds)
                assertEquals(setOf("MAIN_CHAIN", "LIGHTNING"), savedConfig.selectedSettlementMethodIds)
                assertFalse(savedConfig.hasManualPaymentFilter)
                assertFalse(savedConfig.hasManualSettlementFilter)
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    @Test
    fun `when payment and settlement selections are set then config is persisted for current market`() =
        runTest(testDispatcher) {
            val fixture = createFixture()
            try {
                awaitBaseline(fixture.presenter)

                fixture.presenter.setSelectedPaymentMethodIds(setOf("WISE"))
                fixture.presenter.setSelectedSettlementMethodIds(setOf("LIGHTNING"))
                awaitSelected(fixture.presenter, setOf("WISE"), setOf("LIGHTNING"))
                advanceUntilIdle()

                val savedConfig = fixture.repository.getConfig("BTC/USD")
                assertEquals(setOf("WISE"), savedConfig.selectedPaymentMethodIds)
                assertEquals(setOf("LIGHTNING"), savedConfig.selectedSettlementMethodIds)
                assertTrue(savedConfig.hasManualPaymentFilter)
                assertTrue(savedConfig.hasManualSettlementFilter)
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    @Test
    fun `when methods are toggled then config is persisted for current market`() =
        runTest(testDispatcher) {
            val fixture = createFixture()
            try {
                awaitBaseline(fixture.presenter)
                awaitSelected(fixture.presenter, setOf("SEPA", "WISE"), setOf("MAIN_CHAIN", "LIGHTNING"))

                fixture.presenter.togglePaymentMethod("SEPA")
                fixture.presenter.toggleSettlementMethod("MAIN_CHAIN")
                awaitSelected(fixture.presenter, setOf("WISE"), setOf("LIGHTNING"))
                advanceUntilIdle()

                val savedConfig = fixture.repository.getConfig("BTC/USD")
                assertEquals(setOf("WISE"), savedConfig.selectedPaymentMethodIds)
                assertEquals(setOf("LIGHTNING"), savedConfig.selectedSettlementMethodIds)
                assertTrue(savedConfig.hasManualPaymentFilter)
                assertTrue(savedConfig.hasManualSettlementFilter)
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    @Test
    fun `when filters are cleared then default selections and onlyMyOffers are persisted`() =
        runTest(testDispatcher) {
            val fixture = createFixture()
            try {
                awaitBaseline(fixture.presenter)
                fixture.presenter.setOnlyMyOffers(true)
                fixture.presenter.setSelectedPaymentMethodIds(setOf("WISE"))
                fixture.presenter.setSelectedSettlementMethodIds(setOf("LIGHTNING"))
                awaitSelected(fixture.presenter, setOf("WISE"), setOf("LIGHTNING"))
                advanceUntilIdle()

                fixture.presenter.clearAllFilters()
                awaitBaseline(fixture.presenter)
                awaitSelected(fixture.presenter, setOf("SEPA", "WISE"), setOf("MAIN_CHAIN", "LIGHTNING"))
                advanceUntilIdle()

                val savedConfig = fixture.repository.getConfig("BTC/USD")
                assertEquals(setOf("SEPA", "WISE"), savedConfig.selectedPaymentMethodIds)
                assertEquals(setOf("MAIN_CHAIN", "LIGHTNING"), savedConfig.selectedSettlementMethodIds)
                assertFalse(savedConfig.onlyMyOffers)
                assertFalse(savedConfig.hasManualPaymentFilter)
                assertFalse(savedConfig.hasManualSettlementFilter)
            } finally {
                fixture.presenter.onViewUnattaching()
                advanceUntilIdle()
            }
        }

    private fun defaultOffers(): List<OfferItemPresentationModel> =
        listOf(
            makeOffer("sepa", quoteMethods = listOf("SEPA"), baseMethods = listOf("MAIN_CHAIN")),
            makeOffer("wise", quoteMethods = listOf("WISE"), baseMethods = listOf("LIGHTNING")),
        )

    private fun makeOffer(
        id: String,
        quoteMethods: List<String>,
        baseMethods: List<String>,
    ): OfferItemPresentationModel {
        val market = market("USD")
        val amountSpec = QuoteSideRangeAmountSpecVO(minAmount = 10_0000L, maxAmount = 100_0000L)
        val priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_00L, market) })
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = id, hash = id, id = id),
            )
        val offer =
            BisqEasyOfferVO(
                id = id,
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.SELL,
                market = market,
                amountSpec = amountSpec,
                priceSpec = priceSpec,
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = listOf("en"),
            )
        val dto =
            OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = false,
                userProfile = createMockUserProfile("maker-$id"),
                formattedDate = "",
                formattedQuoteAmount = "",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = quoteMethods,
                baseSidePaymentMethods = baseMethods,
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            )
        return OfferItemPresentationModel(dto)
    }

    private fun market(quoteCurrencyCode: String): MarketVO =
        MarketVO(
            baseCurrencyCode = "BTC",
            quoteCurrencyCode = quoteCurrencyCode,
            baseCurrencyName = "Bitcoin",
            quoteCurrencyName = quoteCurrencyCode,
        )

    private fun testMarketPriceServiceFacade(): MarketPriceServiceFacade =
        object : MarketPriceServiceFacade(mockk(relaxed = true)) {
            override fun findMarketPriceItem(marketVO: MarketVO) = null

            override fun findUSDMarketPriceItem() = null

            override fun refreshSelectedFormattedMarketPrice() {}

            override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
        }
}
