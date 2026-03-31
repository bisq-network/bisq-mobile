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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.probeStateFlow
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
            val uiStateProbe = probeStateFlow(presenter.uiState)

            advanceUntilIdle()
            val uiState = uiStateProbe.latest()

            assertEquals("emergency-new", uiState.currentAlert?.id)
            assertEquals(3, uiState.pendingAlertCount)
            assertTrue(uiState.isBannerVisible)

            uiStateProbe.cancel()
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
            val uiStateProbe = probeStateFlow(presenter.uiState)

            advanceUntilIdle()
            val dialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("warn"))
            advanceUntilIdle()

            assertEquals(1, uiStateProbe.valuesSince(dialogMark).size)

            val dialogState = uiStateProbe.latest().currentAlertDialog
            assertEquals("warn", dialogState?.id)
            assertEquals(AlertType.WARN, dialogState?.type)
            assertEquals("Headline", dialogState?.headline)
            assertEquals("message", dialogState?.message)

            uiStateProbe.cancel()
        }

    @Test
    fun `close dialog clears expanded alert`() =
        runTest(testDispatcher) {
            val alertsFlow = MutableStateFlow(listOf(alert(id = "warn", type = AlertType.WARN, date = 5L)))
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)
            val uiStateProbe = probeStateFlow(presenter.uiState)

            advanceUntilIdle()
            val openDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("warn"))
            advanceUntilIdle()
            assertEquals(1, uiStateProbe.valuesSince(openDialogMark).size)
            assertEquals(
                "warn",
                uiStateProbe.latest().currentAlertDialog?.id,
            )

            val closeDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.OnCloseDialog)
            advanceUntilIdle()

            assertEquals(1, uiStateProbe.valuesSince(closeDialogMark).size)
            assertEquals(
                null,
                uiStateProbe.latest().currentAlertDialog,
            )

            uiStateProbe.cancel()
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
            val uiStateProbe = probeStateFlow(presenter.uiState)

            advanceUntilIdle()
            assertFalse(uiStateProbe.latest().isBannerVisible)

            val visibilityChangeMark = uiStateProbe.mark()
            mainPresenter.setIsMainContentVisible(true)
            advanceUntilIdle()

            assertEquals(1, uiStateProbe.valuesSince(visibilityChangeMark).size)
            assertTrue(uiStateProbe.latest().isBannerVisible)

            uiStateProbe.cancel()
        }

    @Test
    fun `default headlines are applied for visible alert types and highest severity ranks first`() =
        runTest(testDispatcher) {
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(id = "info", type = AlertType.INFO, date = 1L, headline = null),
                        alert(id = "warn", type = AlertType.WARN, date = 2L, headline = null),
                        alert(id = "emergency", type = AlertType.EMERGENCY, date = 3L, headline = null),
                    ),
                )
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            mainPresenter.setIsMainContentVisible(true)

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)
            val uiStateProbe = probeStateFlow(presenter.uiState)

            advanceUntilIdle()
            assertEquals("emergency", uiStateProbe.latest().currentAlert?.id)

            val infoDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("info"))
            advanceUntilIdle()
            assertEquals(1, uiStateProbe.valuesSince(infoDialogMark).size)
            assertEquals(
                "authorizedRole.securityManager.alertType.INFO".i18n(),
                uiStateProbe.latest().currentAlertDialog?.headline,
            )

            val warnDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("warn"))
            advanceUntilIdle()
            assertEquals(1, uiStateProbe.valuesSince(warnDialogMark).size)
            assertEquals(
                "authorizedRole.securityManager.alertType.WARN".i18n(),
                uiStateProbe.latest().currentAlertDialog?.headline,
            )

            val emergencyDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("emergency"))
            advanceUntilIdle()
            assertEquals(1, uiStateProbe.valuesSince(emergencyDialogMark).size)
            assertEquals(
                "authorizedRole.securityManager.alertType.EMERGENCY".i18n(),
                uiStateProbe.latest().currentAlertDialog?.headline,
            )

            uiStateProbe.cancel()
        }

    @Test
    fun `non message alert types are never exposed in banner or dialog ui state`() =
        runTest(testDispatcher) {
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(id = "ban", type = AlertType.BAN, date = 100L),
                        alert(id = "banned-account", type = AlertType.BANNED_ACCOUNT_DATA, date = 200L),
                        alert(id = "warn", type = AlertType.WARN, date = 50L),
                    ),
                )
            val alertServiceFacade = FakeAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            mainPresenter.setIsMainContentVisible(true)

            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertServiceFacade)
            val uiStateProbe = probeStateFlow(presenter.uiState)

            advanceUntilIdle()

            val uiState = uiStateProbe.latest()
            assertEquals("warn", uiState.currentAlert?.id)
            assertEquals(0, uiState.pendingAlertCount)
            assertTrue(uiState.isBannerVisible)

            val hiddenDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("ban"))
            advanceUntilIdle()
            assertTrue(uiStateProbe.valuesSince(hiddenDialogMark).isEmpty())
            assertEquals(null, uiStateProbe.latest().currentAlertDialog)

            val hiddenAccountDialogMark = uiStateProbe.mark()
            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("banned-account"))
            advanceUntilIdle()
            assertTrue(uiStateProbe.valuesSince(hiddenAccountDialogMark).isEmpty())
            assertEquals(null, uiStateProbe.latest().currentAlertDialog)

            val hiddenOnlyMark = uiStateProbe.mark()
            alertsFlow.value = listOf(alert(id = "ban-only", type = AlertType.BAN, date = 300L))
            advanceUntilIdle()

            assertEquals(1, uiStateProbe.valuesSince(hiddenOnlyMark).size)

            val hiddenOnlyUiState = uiStateProbe.latest()
            assertEquals(null, hiddenOnlyUiState.currentAlert)
            assertEquals(0, hiddenOnlyUiState.pendingAlertCount)
            assertFalse(hiddenOnlyUiState.isBannerVisible)
            assertEquals(null, hiddenOnlyUiState.currentAlertDialog)

            uiStateProbe.cancel()
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
