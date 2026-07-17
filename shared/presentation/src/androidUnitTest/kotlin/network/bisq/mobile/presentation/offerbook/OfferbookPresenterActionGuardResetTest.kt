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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
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
import network.bisq.mobile.domain.repository.SettingsRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [OfferbookPresenter] is bound as a Koin `factory` and driven by
 * [network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware], so
 * `onViewAttached()` runs once and every later return from the back stack fires `onViewRevealed()`.
 * Action guards must reset on reveal so a prior navigation or in-flight action (take/create offer,
 * which deliberately leave their guard disabled) cannot leave the FAB/cards permanently disabled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterActionGuardResetTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `onViewRevealed resets isCreateOfferEnabled after create offer navigation`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.createOffer()
            advanceUntilIdle()

            assertFalse(presenter.isCreateOfferEnabled.value)

            presenter.onViewRevealed()
            assertTrue(presenter.isCreateOfferEnabled.value)
        }

    @Test
    fun `onViewRevealed resets isTakeOfferEnabled when guard was left disabled`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            setActionGuardEnabled(presenter, "_isTakeOfferEnabled", enabled = false)
            assertFalse(presenter.isTakeOfferEnabled.value)

            presenter.onViewRevealed()
            assertTrue(presenter.isTakeOfferEnabled.value)
        }

    @Test
    fun `onViewRevealed resets all action guards`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            setActionGuardEnabled(presenter, "_isCreateOfferEnabled", enabled = false)
            setActionGuardEnabled(presenter, "_isDeleteOfferEnabled", enabled = false)
            setActionGuardEnabled(presenter, "_isTakeOfferEnabled", enabled = false)

            presenter.onViewRevealed()

            assertTrue(presenter.isCreateOfferEnabled.value)
            assertTrue(presenter.isDeleteOfferEnabled.value)
            assertTrue(presenter.isTakeOfferEnabled.value)
        }

    @Test
    fun `onViewRevealed resets isDeleteOfferEnabled while delete is in progress`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val offersService = mockk<OffersServiceFacade>(relaxed = true)
            coEvery { offersService.deleteOffer(any()) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(true)
            }

            val presenter =
                buildPresenter(
                    offers = listOf(myOffer),
                    offersService = offersService,
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onOfferSelected(myOffer)
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()

            assertFalse(presenter.isDeleteOfferEnabled.value)

            presenter.onViewRevealed()
            assertTrue(presenter.isDeleteOfferEnabled.value)
        }

    @Test
    fun `onViewRevealed hides delete confirmation so it cannot reopen against a null selection`() =
        runTest(testDispatcher) {
            val myOffer = makeOffer(id = "my-offer", isMy = true)
            val presenter = buildPresenter(offers = listOf(myOffer))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onOfferSelected(myOffer)
            assertTrue(presenter.showDeleteConfirmation.value)

            // Simulate a config change (rotation/dark mode) while the delete dialog is open:
            // the surviving presenter is revealed and must clear the dialog along with the selection.
            presenter.onViewRevealed()

            assertFalse(presenter.showDeleteConfirmation.value)
        }

    @Test
    fun `onViewAttached resets action guards on first attach`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()

            setActionGuardEnabled(presenter, "_isCreateOfferEnabled", enabled = false)
            setActionGuardEnabled(presenter, "_isDeleteOfferEnabled", enabled = false)
            setActionGuardEnabled(presenter, "_isTakeOfferEnabled", enabled = false)

            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.isCreateOfferEnabled.value)
            assertTrue(presenter.isDeleteOfferEnabled.value)
            assertTrue(presenter.isTakeOfferEnabled.value)
        }

    private fun buildPresenter(
        offers: List<OfferItemPresentationModel> = emptyList(),
        offersService: OffersServiceFacade = mockk(relaxed = true),
        takeOfferCoordinator: TakeOfferCoordinator = mockk(relaxed = true),
        createOfferCoordinator: CreateOfferCoordinator = mockk(relaxed = true),
        reputationService: ReputationServiceFacade = mockk(relaxed = true),
    ): OfferbookPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )

        val offersFlow = MutableStateFlow(offers)
        val market =
            OfferbookMarket(
                MarketVO("BTC", "USD", "Bitcoin", "US Dollar"),
            )
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns MutableStateFlow(market)
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
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
        val marketPriceServiceFacade =
            object : MarketPriceServiceFacade(settingsRepository) {
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

        coEvery { reputationService.getReputation(any()) } returns
            Result.success(
                ReputationScoreVO(totalScore = 999_999L, fiveSystemScore = 5.0, ranking = 1),
            )

        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)
        every { tradeRestrictingAlertServiceFacade.alert } returns MutableStateFlow(null)

        val offerbookFilterConfigRepository = mockk<OfferbookFilterConfigRepository>(relaxed = true)
        coEvery { offerbookFilterConfigRepository.getConfig(any()) } returns OfferbookFilterConfig()
        coEvery { offerbookFilterConfigRepository.setConfig(any(), any()) } returns Unit

        return OfferbookPresenter(
            mainPresenter,
            offersService,
            takeOfferCoordinator,
            createOfferCoordinator,
            marketPriceServiceFacade,
            userProfileServiceFacade,
            reputationService,
            tradeRestrictingAlertServiceFacade,
            offerbookFilterConfigRepository,
            configServiceFacade = FakeConfigServiceFacade(),
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

    private fun setActionGuardEnabled(
        presenter: OfferbookPresenter,
        fieldName: String,
        enabled: Boolean,
    ) {
        val field = OfferbookPresenter::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(presenter) as MutableStateFlow<Boolean>).value = enabled
    }
}
