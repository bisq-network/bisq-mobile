package network.bisq.mobile.presentation.tabs.tab

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the trade-restriction dialog flow in [TabContainerPresenter].
 *
 * Mirrors [OfferbookPresenterTradeRestrictionTest] for the identical branching in
 * [TabContainerPresenter.createOffer] and [TabContainerPresenter.onTradeRestrictingAlertAction].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabContainerPresenterTradeRestrictionTest {
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

    private fun buildPresenter(activeAlert: AuthorizedAlertData? = null): TabContainerPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())
        val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)

        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(false)

        val alertFlow = MutableStateFlow<AuthorizedAlertData?>(activeAlert)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>()
        every { tradeRestrictingAlertServiceFacade.alert } returns alertFlow

        return TabContainerPresenter(
            mainPresenter = mainPresenter,
            createOfferCoordinator = createOfferCoordinator,
            settingsServiceFacade = settingsServiceFacade,
            tradeRestrictingAlertServiceFacade = tradeRestrictingAlertServiceFacade,
        )
    }

    private fun makeAlert(headline: String = "Trading restricted") =
        AuthorizedAlertData(
            id = "tab-alert",
            type = AlertType.EMERGENCY,
            headline = headline,
            message = "Please update the app.",
            haltTrading = true,
            date = 1L,
        )

    // -------------------------------------------------------------------------
    // Active-alert gating on createOffer
    // -------------------------------------------------------------------------

    @Test
    fun `showTradeRestrictedDialog is null on construction`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    @Test
    fun `createOffer with active alert sets showTradeRestrictedDialog`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert())
            assertNull(presenter.showTradeRestrictedDialog.value)

            presenter.createOffer()
            advanceUntilIdle()

            val state = presenter.showTradeRestrictedDialog.value
            assertNotNull(state)
        }

    @Test
    fun `createOffer without active alert leaves showTradeRestrictedDialog null`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = null)
            // createOfferCoordinator is relaxed so navigation doesn't crash; dialog must stay null.
            presenter.createOffer()
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // OnUpdateNow
    // -------------------------------------------------------------------------

    @Test
    fun `OnUpdateNow clears showTradeRestrictedDialog`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert())
            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnUpdateNow)
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // OnCloseDialog
    // -------------------------------------------------------------------------

    @Test
    fun `OnCloseDialog dismisses dialog without triggering update`() =
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
    // Round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `dialog can be reopened after being closed`() =
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
