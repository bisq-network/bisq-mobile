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
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.domain.service.offers.OffersBelowReputationService
import network.bisq.mobile.domain.service.offers.OffersBelowReputationService.RemoveOutcome
import network.bisq.mobile.domain.service.offers.OffersBelowReputationState
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
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
        mockk<OffersBelowReputationService>(relaxed = true) {
            every { state } returns serviceState
            coEvery { removeOffers() } returns RemoveOutcome.REMOVED_ALL
        }

    @Test
    fun `the banner mirrors the service`() =
        runTest {
            withPresenter { presenter ->
                val state = presenter.offersBelowReputationUiState.value
                assertEquals(listOf(offer), state.offendingOffers)
                assertTrue(state.isBannerVisible)
                assertFalse(state.isDialogVisible)
            }
        }

    @Test
    fun `attaching re-checks my offers`() =
        runTest {
            withPresenter {
                verify { service.refresh() }
            }
        }

    @Test
    fun `tapping the banner opens the dialog`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
                runCurrent()

                assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `dismissing the banner dismisses the offers`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.DismissBanner)

                verify { service.dismiss() }
            }
        }

    @Test
    fun `keep dismisses the offers and closes the dialog`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.Keep)
                runCurrent()

                verify { service.dismiss() }
                assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `closing the dialog without choosing leaves the banner`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.CloseDialog)
                runCurrent()

                verify(exactly = 0) { service.dismiss() }
                val state = presenter.offersBelowReputationUiState.value
                assertFalse(state.isDialogVisible)
                assertTrue(state.isBannerVisible)
            }
        }

    @Test
    fun `build reputation closes the dialog and navigates`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.BuildReputation)
                runCurrent()

                verify { navigationManager.navigate(NavRoute.Reputation, any(), any()) }
                assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `remove asks the service and the dialog closes once no offer is left`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
                runCurrent()
                serviceState.value = OffersBelowReputationState()
                runCurrent()

                coVerify(exactly = 1) { service.removeOffers() }
                assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
                verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
            }
        }

    @Test
    fun `an offer flagged after removing them all does not reopen the dialog`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
                runCurrent()
                serviceState.value = OffersBelowReputationState()
                runCurrent()

                serviceState.value = OffersBelowReputationState(offendingOffers = listOf(offer.copy(offerId = "offer-2")), isWarningVisible = true)
                runCurrent()

                assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `tapping the banner once no offer is left does not open the dialog later`() =
        runTest {
            withPresenter { presenter ->
                serviceState.value = OffersBelowReputationState()
                runCurrent()
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                serviceState.value = OffersBelowReputationState(offendingOffers = listOf(offer.copy(offerId = "offer-2")), isWarningVisible = true)
                runCurrent()

                assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `remove in demo mode shows a snackbar and removes nothing`() =
        runTest {
            withPresenter(mainPresenter = mockk<MainPresenter>(relaxed = true) { every { isDemo() } returns true }) { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
                runCurrent()

                coVerify(exactly = 0) { service.removeOffers() }
                verify(exactly = 1) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
            }
        }

    @Test
    fun `a failed remove shows a snackbar and keeps the dialog open`() =
        runTest {
            coEvery { service.removeOffers() } returns RemoveOutcome.SOME_FAILED
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
                runCurrent()

                verify(exactly = 1) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
                assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `a remove that was already running shows no snackbar`() =
        runTest {
            coEvery { service.removeOffers() } returns RemoveOutcome.ALREADY_RUNNING
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
                runCurrent()

                verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
                assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `decisions are ignored while removing`() =
        runTest {
            coEvery { service.removeOffers() } coAnswers { CompletableDeferred<RemoveOutcome>().await() }
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
                serviceState.update { it.copy(isRemoving = true) }
                runCurrent()

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.Keep)
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.DismissBanner)
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.BuildReputation)
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.RemoveOffers)
                runCurrent()

                verify(exactly = 0) { service.dismiss() }
                verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }
                coVerify(exactly = 0) { service.removeOffers() }
                assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
            }
        }

    @Test
    fun `the dialog can be closed and reopened while removing`() =
        runTest {
            withPresenter { presenter ->
                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
                serviceState.update { it.copy(isRemoving = true) }
                runCurrent()

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.CloseDialog)
                runCurrent()
                assertFalse(presenter.offersBelowReputationUiState.value.isDialogVisible)

                presenter.onOffersBelowReputationAction(OffersBelowReputationUiAction.OpenDialog)
                runCurrent()
                assertTrue(presenter.offersBelowReputationUiState.value.isDialogVisible)
                assertTrue(presenter.offersBelowReputationUiState.value.isBannerVisible)
            }
        }

    // Detaches afterwards, so the collectors launched on attach do not outlive the test.
    private fun TestScope.withPresenter(
        mainPresenter: MainPresenter = MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService()),
        block: (TabContainerPresenter) -> Unit,
    ) {
        val presenter = attachPresenter(mainPresenter)
        try {
            block(presenter)
        } finally {
            presenter.onViewUnattaching()
            runCurrent()
        }
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
            )
        presenter.onViewAttached()
        runCurrent()
        return presenter
    }
}
