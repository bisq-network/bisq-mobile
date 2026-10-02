package network.bisq.mobile.presentation.tabs.tab

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.domain.service.offers.OffersBelowReputationState
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.testOffersBelowReputationService
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offers_below_reputation.OffersBelowReputationUiAction
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The tab shell's warning about my sell offers below reputation: banner, review dialog and its actions. */
@OptIn(ExperimentalCoroutinesApi::class)
class TabContainerPresenterOffersBelowReputationTest : PlatformPresentationKoinTestBase() {
    private val offer = OffendingOffer(offerId = "offer-1", market = MarketVOFactory.USD, formattedAmount = "50 USD")
    private val serviceState = MutableStateFlow(OffersBelowReputationState(offendingOffers = listOf(offer), isWarningVisible = true))
    private val service =
        testOffersBelowReputationService(serviceState).also {
            coEvery { it.removeOffers() } returns true
        }
    private val offersService =
        mockk<OffersServiceFacade> {
            every { selectOfferbookMarket(any()) } returns Result.success(Unit)
        }

    @Test
    fun `the banner mirrors the service`() =
        runTest {
            val presenter = attachPresenter()

            val state = presenter.offersBelowReputationUiState.value
            assertEquals(listOf(offer), state.offendingOffers)
            assertTrue(state.isBannerVisible)
            assertFalse(state.isDialogVisible)
        }

    @Test
    fun `attaching re-checks my offers`() =
        runTest {
            attachPresenter()

            verify { service.refresh() }
        }

    @Test
    fun `tapping the banner opens the dialog`() =
        runTest {
            val presenter = attachPresenter()

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
            runCurrent()

            assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
        }

    @Test
    fun `dismissing the banner dismisses the offers`() =
        runTest {
            val presenter = attachPresenter()

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.DismissBanner)

            verify { service.dismiss() }
        }

    @Test
    fun `keep dismisses the offers and closes the dialog`() =
        runTest {
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.Keep)
            runCurrent()

            verify { service.dismiss() }
            assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
        }

    @Test
    fun `build reputation closes the dialog and navigates`() =
        runTest {
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.BuildReputation)
            runCurrent()

            verify { navigationManager.navigate(NavRoute.Reputation, any(), any()) }
            assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
        }

    @Test
    fun `remove asks the service and the dialog closes once no offer is left`() =
        runTest {
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
            runCurrent()
            serviceState.value = OffersBelowReputationState()
            runCurrent()

            coVerify(exactly = 1) { service.removeOffers() }
            assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `an offer flagged after removing them all does not reopen the dialog`() =
        runTest {
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
            runCurrent()
            serviceState.value = OffersBelowReputationState()
            runCurrent()

            serviceState.value = OffersBelowReputationState(offendingOffers = listOf(offer.copy(offerId = "offer-2")), isWarningVisible = true)
            runCurrent()

            assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
        }

    @Test
    fun `remove in demo mode shows a snackbar and removes nothing`() =
        runTest {
            val presenter = attachPresenter(mainPresenter = mockk<MainPresenter>(relaxed = true) { every { isDemo() } returns true })

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
            runCurrent()

            coVerify(exactly = 0) { service.removeOffers() }
            verify(exactly = 1) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `a failed remove shows a snackbar and keeps the dialog open`() =
        runTest {
            coEvery { service.removeOffers() } returns false
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
            runCurrent()

            verify(exactly = 1) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
            assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
        }

    @Test
    fun `actions are ignored while removing`() =
        runTest {
            coEvery { service.removeOffers() } coAnswers { CompletableDeferred<Boolean>().await() }
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
            serviceState.update { it.copy(isRemoving = true) }
            runCurrent()

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.Keep)
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.BuildReputation)
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
            runCurrent()

            verify(exactly = 0) { service.dismiss() }
            verify(exactly = 0) { navigationManager.navigate(NavRoute.Reputation, any(), any()) }
            coVerify(exactly = 0) { service.removeOffers() }
            assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
        }

    @Test
    fun `go to market opens that market with only my offers and closes the dialog`() =
        runTest {
            val presenter = attachPresenter()
            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.GoToMarket(offer))
            runCurrent()

            verify { offersService.selectOfferbookMarket(match { it.market == offer.market }) }
            verify { navigationManager.navigate(NavRoute.Offerbook(onlyMyOffers = true), any(), any()) }
            assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
            verify(exactly = 0) { service.dismiss() }
        }

    @Test
    fun `go to market stays put when the market cannot be selected`() =
        runTest {
            every { offersService.selectOfferbookMarket(any()) } returns Result.failure(IllegalStateException("boom"))
            val presenter = attachPresenter()

            presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.GoToMarket(offer))
            runCurrent()

            verify(exactly = 0) { navigationManager.navigate(any<NavRoute.Offerbook>(), any(), any()) }
            verify(exactly = 1) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    private fun TestScope.attachPresenter(
        mainPresenter: MainPresenter = MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService()),
    ): TabContainerPresenter {
        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(false)
        val presenter =
            TabContainerPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true),
                settingsServiceFacade = settingsServiceFacade,
                tradeRestrictingAlertServiceFacade =
                    mockk<TradeRestrictingAlertServiceFacade> {
                        every { alert } returns MutableStateFlow(null)
                    },
                appUpdateLinker = FakeAppUpdateLinker(),
                animationSettings = AnimationSettings(settingsServiceFacade, mockk(relaxed = true), applyDeviceLock = false),
                communityHubService =
                    mockk<CommunityHubService>(relaxed = true) {
                        every { liveSegments } returns MutableStateFlow(emptySet())
                        every { unreadCount } returns MutableStateFlow(0)
                    },
                offersBelowReputationService = service,
                offersServiceFacade = offersService,
            )
        presenter.onViewAttached()
        runCurrent()
        return presenter
    }
}
