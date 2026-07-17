package network.bisq.mobile.presentation.offerbook

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.di.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
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

/**
 * The offerbook shows a "still loading" snackbar when the offers stay loading for more than 5s
 * (common on slow/Tor cold starts). Verifies the hint fires after the threshold, and is suppressed
 * if loading finishes first (the collectLatest cancels the pending delay).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterSlowLoadingHintTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var globalUiManager: GlobalUiManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        globalUiManager = mockk(relaxed = true)
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single<GlobalUiManager> { globalUiManager }
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
    fun `shows slow-loading hint when offers stay loading past the threshold`() =
        runTest(testDispatcher) {
            val loading = MutableStateFlow(true)
            val presenter = buildPresenter(loading)

            presenter.onViewAttached()
            advanceTimeBy(5_100L)
            advanceUntilIdle()

            coVerify {
                globalUiManager.showSnackbar(
                    "mobile.offerbook.slowLoadingHint".i18n(),
                    SnackbarType.WARNING,
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `does not show slow-loading hint when offers finish loading before the threshold`() =
        runTest(testDispatcher) {
            val loading = MutableStateFlow(true)
            val presenter = buildPresenter(loading)

            presenter.onViewAttached()
            advanceTimeBy(2_000L)
            loading.value = false
            advanceTimeBy(5_000L)
            advanceUntilIdle()

            coVerify(exactly = 0) {
                globalUiManager.showSnackbar(
                    "mobile.offerbook.slowLoadingHint".i18n(),
                    SnackbarType.WARNING,
                    any(),
                    any(),
                )
            }
        }

    private fun buildPresenter(loadingFlow: MutableStateFlow<Boolean>): OfferbookPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )

        val offersService = mockk<OffersServiceFacade>(relaxed = true)
        val market = OfferbookMarket(MarketVO("BTC", "USD", "Bitcoin", "US Dollar"))
        every { offersService.offerbookListItems } returns MutableStateFlow(emptyList<OfferItemPresentationModel>())
        every { offersService.selectedOfferbookMarket } returns MutableStateFlow(market)
        every { offersService.isOfferbookLoading } returns loadingFlow

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(createMockUserProfile("me"))
        coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false

        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val marketPriceServiceFacade =
            object : MarketPriceServiceFacade(settingsRepository) {
                override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? = null

                override fun findUSDMarketPriceItem(): MarketPriceItem? = null

                override fun refreshSelectedFormattedMarketPrice() {}

                override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
            }

        val reputationService = mockk<ReputationServiceFacade>(relaxed = true)
        coEvery { reputationService.getReputation(any()) } returns
            Result.success(ReputationScoreVO(totalScore = 999_999L, fiveSystemScore = 5.0, ranking = 1))

        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)
        every { tradeRestrictingAlertServiceFacade.alert } returns MutableStateFlow(null)

        val offerbookFilterConfigRepository = mockk<OfferbookFilterConfigRepository>(relaxed = true)
        coEvery { offerbookFilterConfigRepository.getConfig(any()) } returns OfferbookFilterConfig()
        coEvery { offerbookFilterConfigRepository.setConfig(any(), any()) } returns Unit

        return OfferbookPresenter(
            mainPresenter,
            offersService,
            mockk<TakeOfferCoordinator>(relaxed = true),
            mockk<CreateOfferCoordinator>(relaxed = true),
            marketPriceServiceFacade,
            userProfileServiceFacade,
            reputationService,
            tradeRestrictingAlertServiceFacade,
            offerbookFilterConfigRepository,
            configServiceFacade = FakeConfigServiceFacade(),
        )
    }
}
