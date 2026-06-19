package network.bisq.mobile.presentation.offerbook

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.model.offerbook.MarketListItem
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
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfferbookPresenterGuardedActionsTest {
    private val testDispatcher = StandardTestDispatcher()
    private var previousDemoState = false

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        previousDemoState = ApplicationBootstrapFacade.isDemo
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        ApplicationBootstrapFacade.isDemo = previousDemoState
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `onConfirmedDeleteOffer with no selected offer shows error snackbar`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()
            assertFalse(presenter.showDeleteConfirmation.value)
        }

    @Test
    fun `onConfirmedDeleteOffer in demo mode does not call delete service`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val offersService = mockk<OffersServiceFacade>(relaxed = true)
            every { offersService.offerbookListItems } returns MutableStateFlow(listOf(myOffer))
            every { offersService.selectedOfferbookMarket } returns
                MutableStateFlow(OfferbookMarket(MarketVO("BTC", "USD", "Bitcoin", "US Dollar")))
            every { offersService.isOfferbookLoading } returns MutableStateFlow(false)

            val demoMainPresenter =
                mockk<MainPresenter>(relaxed = true) {
                    every { isDemo() } returns true
                }
            val presenter =
                buildPresenter(
                    offers = listOf(myOffer),
                    offersService = offersService,
                    mainPresenter = demoMainPresenter,
                )
            presenter.onOfferSelected(myOffer)
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()

            coVerify(exactly = 0) { offersService.deleteOffer(any()) }
        }

    @Test
    fun `onConfirmedDeleteOffer failure re-enables delete guard and deselects offer`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val offersService = mockk<OffersServiceFacade>(relaxed = true)
            coEvery { offersService.deleteOffer(any()) } returns Result.failure(RuntimeException("network"))

            val presenter = buildPresenter(offers = listOf(myOffer), offersService = offersService)
            presenter.onOfferSelected(myOffer)
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()

            assertTrue(presenter.isDeleteOfferEnabled.value)
        }

    @Test
    fun `onConfirmedDeleteOffer when delete returns false shows error snackbar`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val offersService = mockk<OffersServiceFacade>(relaxed = true)
            coEvery { offersService.deleteOffer(any()) } returns Result.success(false)

            val presenter = buildPresenter(offers = listOf(myOffer), offersService = offersService)
            presenter.onOfferSelected(myOffer)
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()

            assertTrue(presenter.isDeleteOfferEnabled.value)
        }

    @Test
    fun `onDismissDeleteOffer clears confirmation and deselects offer`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val presenter = buildPresenter(offers = listOf(myOffer))

            presenter.onOfferSelected(myOffer)
            assertTrue(presenter.showDeleteConfirmation.value)

            presenter.onDismissDeleteOffer()
            assertFalse(presenter.showDeleteConfirmation.value)
        }

    @Test
    fun `onConfirmedDeleteOffer service exception triggers failure handler`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val offersService = mockk<OffersServiceFacade>(relaxed = true)
            every { offersService.offerbookListItems } returns MutableStateFlow(listOf(myOffer))
            every { offersService.selectedOfferbookMarket } returns
                MutableStateFlow(OfferbookMarket(MarketVO("BTC", "USD", "Bitcoin", "US Dollar")))
            every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
            coEvery { offersService.deleteOffer(any()) } throws RuntimeException("boom")

            val presenter = buildPresenter(offers = listOf(myOffer), offersService = offersService)
            presenter.onOfferSelected(myOffer)
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()

            assertTrue(presenter.isDeleteOfferEnabled.value)
        }

    @Test
    fun `takeOffer with null selected offer is ignored`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            invokeTakeOffer(presenter)
            advanceUntilIdle()
            assertTrue(presenter.isTakeOfferEnabled.value)
        }

    @Test
    fun `onOfferSelected failure when selected profile is null does not navigate`() =
        runTest(testDispatcher) {
            val otherOffer = makeOffer(id = "other-offer", isMy = false)
            val takeOfferCoordinator = mockk<TakeOfferCoordinator>(relaxed = true)
            val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
            coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false

            val navManager = mockk<NavigationManager>(relaxed = true)
            stopKoin()
            startKoin {
                modules(
                    module {
                        factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                        single<NavigationManager> { navManager }
                        single { GlobalUiManager(testDispatcher) }
                    },
                )
            }

            val presenter =
                buildPresenter(
                    offers = listOf(otherOffer),
                    takeOfferCoordinator = takeOfferCoordinator,
                    userProfileServiceFacade = userProfileServiceFacade,
                )
            presenter.onOfferSelected(otherOffer)
            advanceUntilIdle()

            coVerify(exactly = 0) { takeOfferCoordinator.selectOfferToTake(any()) }
            verify(exactly = 0) { navManager.navigate(any(), any(), any()) }
        }

    @Test
    fun `takeOffer on own offer triggers failure handler and re-enables guard`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val presenter = buildPresenter(offers = listOf(myOffer))

            setSelectedOffer(presenter, myOffer)
            invokeTakeOffer(presenter)
            advanceUntilIdle()

            assertTrue(presenter.isTakeOfferEnabled.value)
            val field = OfferbookPresenter::class.java.getDeclaredField("selectedOffer")
            field.isAccessible = true
            assertNull(field.get(presenter))
        }

    @Test
    fun `createOffer with empty market skips currency selection`() =
        runTest(testDispatcher) {
            val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)
            val emptyMarket = OfferbookMarket(MarketVO("", "", "", ""))
            val offersService = mockk<OffersServiceFacade>(relaxed = true)
            every { offersService.offerbookListItems } returns MutableStateFlow(emptyList())
            every { offersService.selectedOfferbookMarket } returns MutableStateFlow(emptyMarket)
            every { offersService.isOfferbookLoading } returns MutableStateFlow(false)

            val presenter =
                buildPresenter(
                    offersService = offersService,
                    createOfferCoordinator = createOfferCoordinator,
                    selectedMarket = emptyMarket,
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.createOffer()
            advanceUntilIdle()

            verify { createOfferCoordinator.skipCurrency = false }
        }

    @Test
    fun `createOffer failure re-enables create guard`() =
        runTest(testDispatcher) {
            val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)
            every { createOfferCoordinator.onStartCreateOffer() } throws RuntimeException("fail")

            val presenter = buildPresenter(createOfferCoordinator = createOfferCoordinator)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.createOffer()
            advanceUntilIdle()

            assertTrue(presenter.isCreateOfferEnabled.value)
        }

    private fun invokeTakeOffer(presenter: OfferbookPresenter) {
        val method = OfferbookPresenter::class.java.getDeclaredMethod("takeOffer")
        method.isAccessible = true
        method.invoke(presenter)
    }

    private fun setSelectedOffer(
        presenter: OfferbookPresenter,
        offer: OfferItemPresentationModel,
    ) {
        val field = OfferbookPresenter::class.java.getDeclaredField("selectedOffer")
        field.isAccessible = true
        field.set(presenter, offer)
    }

    private fun buildPresenter(
        offers: List<OfferItemPresentationModel> = emptyList(),
        offersService: OffersServiceFacade = mockk(relaxed = true),
        takeOfferCoordinator: TakeOfferCoordinator = mockk(relaxed = true),
        createOfferCoordinator: CreateOfferCoordinator = mockk(relaxed = true),
        reputationService: ReputationServiceFacade = mockk(relaxed = true),
        userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true),
        mainPresenter: MainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            ),
        navigationManager: NavigationManager? = null,
        marketPriceServiceFacade: MarketPriceServiceFacade? = null,
        selectedMarket: OfferbookMarket =
            OfferbookMarket(
                MarketVO("BTC", "USD", "Bitcoin", "US Dollar"),
            ),
    ): OfferbookPresenter {
        val offersFlow = MutableStateFlow(offers)
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns MutableStateFlow(selectedMarket)
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)

        val me = createMockUserProfile("me")
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(me)
        coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false

        val marketUSD = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val marketPriceItem =
            MarketPriceItem(
                marketUSD,
                with(PriceQuoteVOFactory) { fromPrice(100_000_00L, marketUSD) },
                formattedPrice = "100000 USD",
            )
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val resolvedMarketPriceServiceFacade =
            marketPriceServiceFacade
                ?: object : MarketPriceServiceFacade(settingsRepository) {
                    override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? =
                        if (marketVO.baseCurrencyCode == marketUSD.baseCurrencyCode &&
                            marketVO.quoteCurrencyCode == marketUSD.quoteCurrencyCode
                        ) {
                            marketPriceItem
                        } else {
                            null
                        }

                    override fun findUSDMarketPriceItem(): MarketPriceItem? = marketPriceItem

                    override fun refreshSelectedFormattedMarketPrice() {}

                    override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
                }

        if (navigationManager != null) {
            stopKoin()
            startKoin {
                modules(
                    module {
                        factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                        single<NavigationManager> { navigationManager }
                        single { GlobalUiManager(testDispatcher) }
                    },
                )
            }
        }

        coEvery { reputationService.getReputation(any()) } returns
            Result.success(
                ReputationScoreVO(totalScore = 999_999L, fiveSystemScore = 5.0, ranking = 1),
            )

        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)
        every { tradeRestrictingAlertServiceFacade.alert } returns MutableStateFlow(null)

        return OfferbookPresenter(
            mainPresenter,
            offersService,
            takeOfferCoordinator,
            createOfferCoordinator,
            resolvedMarketPriceServiceFacade,
            userProfileServiceFacade,
            reputationService,
            tradeRestrictingAlertServiceFacade,
        )
    }

    private fun makeOffer(
        id: String,
        isMy: Boolean,
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
        val user: UserProfileVO = createMockUserProfile("maker-$makerId")
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
                quoteSidePaymentMethods = listOf("SEPA"),
                baseSidePaymentMethods = listOf("MAIN_CHAIN"),
                reputationScore = ReputationScoreVO(999_999L, 5.0, 1),
            )
        return OfferItemPresentationModel(dto)
    }
}
