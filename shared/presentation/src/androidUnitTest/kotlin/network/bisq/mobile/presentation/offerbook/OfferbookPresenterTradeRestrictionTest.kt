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
import kotlinx.coroutines.flow.StateFlow
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
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.AppUpdateLinker
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.domain.repository.OfferbookFilterConfigRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TEST_APP_UPDATE_URL
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.di.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the trade-restriction dialog flow in [OfferbookPresenter].
 *
 * Exercises:
 * - [createOffer] blocked by active alert → [showTradeRestrictedDialog] becomes non-null
 * - "Take offer" path blocked by active alert → dialog becomes non-null
 * - [AlertNotificationUiAction.OnUpdateNow] → dialog cleared
 * - [AlertNotificationUiAction.OnCloseDialog] → dialog cleared without triggering update
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterTradeRestrictionTest {
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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private class FakeOfferbookFilterConfigRepository : OfferbookFilterConfigRepository {
        private val _data = MutableStateFlow(OfferbookFilterConfigs())
        override val data: StateFlow<OfferbookFilterConfigs> = _data

        override suspend fun getConfig(marketKey: String): OfferbookFilterConfig = _data.value.configsByMarket[marketKey] ?: OfferbookFilterConfig()

        override suspend fun setConfig(
            marketKey: String,
            config: OfferbookFilterConfig,
        ) {
            _data.value = _data.value.copy(configsByMarket = _data.value.configsByMarket + (marketKey to config))
        }
    }

    private fun buildPresenter(
        activeAlert: AuthorizedAlertData? = null,
        urlLauncher: UrlLauncher? = null,
        appUpdateLinker: AppUpdateLinker = FakeAppUpdateLinker(),
    ): OfferbookPresenter {
        val resolvedUrlLauncher =
            urlLauncher ?: mockk<UrlLauncher>().also { launcher ->
                coEvery { launcher.openUrl(any()) } returns true
            }
        val mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
                urlLauncher = resolvedUrlLauncher,
            )

        val offersFlow =
            MutableStateFlow(emptyList<network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel>())
        val marketFlow = MutableStateFlow(OfferbookMarket(MarketVO("BTC", "USD", "Bitcoin", "US Dollar")))
        val offersService = mockk<OffersServiceFacade>(relaxed = true)
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns marketFlow
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
        coEvery { offersService.deleteOffer(any()) } returns Result.success(true)

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        val me = createMockUserProfile("me")
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(me)
        coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false

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

        val alertFlow = MutableStateFlow<AuthorizedAlertData?>(activeAlert)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>()
        every { tradeRestrictingAlertServiceFacade.alert } returns alertFlow

        return OfferbookPresenter(
            mainPresenter,
            offersService,
            takeOfferCoordinator,
            createOfferCoordinator,
            marketPriceServiceFacade,
            userProfileServiceFacade,
            reputationService,
            tradeRestrictingAlertServiceFacade,
            FakeOfferbookFilterConfigRepository(),
            configServiceFacade = FakeConfigServiceFacade(),
            appUpdateLinker = appUpdateLinker,
        )
    }

    private fun makeAlert(
        id: String = "test-alert",
        headline: String = "Trading restricted",
    ) = AuthorizedAlertData(
        id = id,
        type = AlertType.EMERGENCY,
        headline = headline,
        message = "Please update.",
        haltTrading = true,
        date = 1L,
    )

    // -------------------------------------------------------------------------
    // createOffer gating
    // -------------------------------------------------------------------------

    @Test
    fun `createOffer with active alert sets showTradeRestrictedDialog`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert(headline = "Restricted!"))
            assertNull(presenter.showTradeRestrictedDialog.value)

            presenter.createOffer()
            advanceUntilIdle()

            val state = presenter.showTradeRestrictedDialog.value
            assertNotNull(state)
            assertEquals("test-alert", state.id)
            assertEquals("Restricted!", state.headline)
        }

    @Test
    fun `createOffer without active alert leaves showTradeRestrictedDialog null`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = null)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.createOffer()
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // OnUpdateNow action
    // -------------------------------------------------------------------------

    @Test
    fun `OnUpdateNow clears showTradeRestrictedDialog and opens update URL`() =
        runTest(testDispatcher) {
            val appUpdateLinker = mockk<AppUpdateLinker>()
            every { appUpdateLinker.getUpdateUrl() } returns TEST_APP_UPDATE_URL
            val urlLauncher = mockk<UrlLauncher>()
            coEvery { urlLauncher.openUrl(any()) } returns true
            val presenter =
                buildPresenter(
                    activeAlert = makeAlert(),
                    urlLauncher = urlLauncher,
                    appUpdateLinker = appUpdateLinker,
                )

            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnUpdateNow)
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
            verify(exactly = 1) { appUpdateLinker.getUpdateUrl() }
            coVerify(exactly = 1) { urlLauncher.openUrl(TEST_APP_UPDATE_URL) }
        }

    // -------------------------------------------------------------------------
    // OnCloseDialog action
    // -------------------------------------------------------------------------

    @Test
    fun `OnCloseDialog clears showTradeRestrictedDialog without triggering update`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert())

            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnCloseDialog)
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `showTradeRestrictedDialog is null on construction`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // Multiple round-trips
    // -------------------------------------------------------------------------

    @Test
    fun `dialog can be opened then closed then reopened`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert())

            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnCloseDialog)
            advanceUntilIdle()
            assertNull(presenter.showTradeRestrictedDialog.value)

            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)
        }
}
