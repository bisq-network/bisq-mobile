package network.bisq.mobile.presentation.common.ui.alert

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
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
class AlertNotificationBannerPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single { NoopNavigationManager() as network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager }
                    single { GlobalUiManager() }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
    }

    @Test
    fun `selects most severe then most recent alert`() =
        runTest(testDispatcher) {
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(id = "info", type = AlertType.INFO, date = 1L),
                        alert(id = "warn", type = AlertType.WARN, date = 10L),
                        alert(id = "emergency-old", type = AlertType.EMERGENCY, date = 20L),
                        alert(id = "emergency-new", type = AlertType.EMERGENCY, date = 30L),
                    ),
                )
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            mainPresenter.setIsMainContentVisible(true)

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)

            advanceUntilIdle()
            val uiState = presenter.uiState.first { it.currentAlert != null }

            assertEquals("emergency-new", uiState.currentAlert?.id)
            assertEquals(3, uiState.pendingAlertCount)
            assertTrue(uiState.isBannerVisible)
        }

    @Test
    fun `dismiss delegates to facade`() =
        runTest(testDispatcher) {
            val alertsFlow = MutableStateFlow(listOf(alert(id = "warn", type = AlertType.WARN, date = 5L)))
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)
            presenter.onAction(AlertNotificationUiAction.OnDismissAlertNotification("warn"))

            assertEquals("warn", alertServiceFacade.lastDismissedAlertId)
        }

    @Test
    fun `expand alert opens dialog for matching alert`() =
        runTest(testDispatcher) {
            val alertsFlow = MutableStateFlow(listOf(alert(id = "warn", type = AlertType.WARN, date = 5L)))
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)

            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("warn"))

            val dialogState = presenter.uiState.first { it.currentAlertDialog?.id == "warn" }.currentAlertDialog
            assertEquals("warn", dialogState?.id)
            assertEquals(AlertType.WARN, dialogState?.type)
            assertEquals("Headline", dialogState?.headline)
            assertEquals("message", dialogState?.message)
        }

    @Test
    fun `close dialog clears expanded alert`() =
        runTest(testDispatcher) {
            val alertsFlow = MutableStateFlow(listOf(alert(id = "warn", type = AlertType.WARN, date = 5L)))
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)

            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("warn"))
            assertEquals(
                "warn",
                presenter.uiState
                    .first { it.currentAlertDialog?.id == "warn" }
                    .currentAlertDialog
                    ?.id,
            )

            presenter.onAction(AlertNotificationUiAction.OnCloseDialog)

            assertEquals(
                null,
                presenter.uiState.first { it.currentAlert?.id == "warn" && it.currentAlertDialog == null }.currentAlertDialog,
            )
        }

    @Test
    fun `update now opens releases url`() =
        runTest(testDispatcher) {
            val urlLauncher = mockk<UrlLauncher>(relaxed = true)
            val alertsFlow = MutableStateFlow(listOf(alert(id = "emergency", type = AlertType.EMERGENCY, date = 5L)))
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create(urlLauncher = urlLauncher)

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)

            presenter.onAction(AlertNotificationUiAction.OnUpdateNow)
            advanceUntilIdle()

            verify(exactly = 1) { urlLauncher.openUrl(BisqLinks.BISQ_MOBILE_RELEASES) }
        }

    @Test
    fun `banner visibility follows main content visibility`() =
        runTest(testDispatcher) {
            val alertsFlow = MutableStateFlow(listOf(alert(id = "info", type = AlertType.INFO, date = 1L)))
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            mainPresenter.setIsMainContentVisible(false)

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)

            advanceUntilIdle()
            assertFalse(presenter.uiState.first().isBannerVisible)

            mainPresenter.setIsMainContentVisible(true)
            advanceUntilIdle()

            assertTrue(presenter.uiState.first { it.isBannerVisible }.isBannerVisible)
        }

    private fun alert(
        id: String,
        type: AlertType,
        date: Long,
        headline: String? = "Headline",
        message: String = "message",
        haltTrading: Boolean = false,
        requiresVersionForTrading: Boolean = false,
        minVersion: String? = null,
    ): AuthorizedAlertData =
        AuthorizedAlertData(
            id = id,
            type = type,
            headline = headline,
            message = message,
            haltTrading = haltTrading,
            requireVersionForTrading = requiresVersionForTrading,
            minVersion = minVersion,
            date = date,
        )

    private class FakeAlertNotificationsServiceFacade(
        private val alertsFlow: MutableStateFlow<List<AuthorizedAlertData>>,
    ) : AlertNotificationsServiceFacade() {
        var lastDismissedAlertId: String? = null

        override val alerts: StateFlow<List<AuthorizedAlertData>> = alertsFlow.asStateFlow()

        override fun dismissAlert(alertId: String) {
            lastDismissedAlertId = alertId
        }
    }
}
