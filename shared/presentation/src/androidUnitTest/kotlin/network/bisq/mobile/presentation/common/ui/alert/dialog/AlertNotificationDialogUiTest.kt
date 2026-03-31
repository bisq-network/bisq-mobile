package network.bisq.mobile.presentation.common.ui.alert.dialog

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AlertNotificationDialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
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
                    single { NoopNavigationManager() as NavigationManager }
                    single { GlobalUiManager() }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
    }

    @Test
    fun `update dialog renders version details and supports update plus dismiss`() =
        runTest(testDispatcher) {
            val urlLauncher = mockk<UrlLauncher>(relaxed = true)
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(
                            id = "update",
                            type = AlertType.EMERGENCY,
                            date = 5L,
                            headline = "Update required",
                            message = "Install the patched build.",
                            requireVersionForTrading = true,
                            minVersion = "2.1.8",
                        ),
                    ),
                )
            val alertFacade = MutableAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create(urlLauncher = urlLauncher)
            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade)

            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("update"))

            composeTestRule.setContent {
                CompositionLocalProvider(LocalIsTest provides true) {
                    BisqTheme {
                        AlertNotificationDialog(presenter = presenter)
                    }
                }
            }

            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Update required").assertIsDisplayed()
            composeTestRule.onNodeWithText("Install the patched build.").assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.alert.update.minimum".i18n("2.1.8")).assertIsDisplayed()

            composeTestRule.onNodeWithText("mobile.alert.update.button".i18n()).performClick()
            advanceUntilIdle()
            verify(exactly = 1) { urlLauncher.openUrl(BisqLinks.BISQ_MOBILE_RELEASES) }

            composeTestRule
                .onNode(
                    hasClickAction() and hasAnyDescendant(hasText("mobile.alert.actions.dismiss.label".i18n())),
                    useUnmergedTree = true,
                ).performClick()
            advanceUntilIdle()
            composeTestRule.runOnIdle {
                assertEquals("update", alertFacade.lastDismissedAlertId)
                assertNull(presenter.uiState.value.currentAlertDialog)
            }
        }

    @Test
    fun `halt trading dialog shows halt message and can be closed`() =
        runTest(testDispatcher) {
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(
                            id = "halt",
                            type = AlertType.EMERGENCY,
                            date = 9L,
                            headline = "Trading suspended",
                            message = "Critical issue detected.",
                            haltTrading = true,
                        ),
                    ),
                )
            val alertFacade = MutableAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade)

            presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("halt"))

            composeTestRule.setContent {
                CompositionLocalProvider(LocalIsTest provides true) {
                    BisqTheme {
                        AlertNotificationDialog(presenter = presenter)
                    }
                }
            }

            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("mobile.alert.trade.halt".i18n()).assertIsDisplayed()
            composeTestRule.onAllNodesWithText("mobile.alert.update.button".i18n()).assertCountEquals(0)

            composeTestRule
                .onNodeWithContentDescription("mobile.alert.actions.dismiss.description".i18n())
                .performClick()
            advanceUntilIdle()

            composeTestRule.runOnIdle {
                assertNull(presenter.uiState.value.currentAlertDialog)
            }
        }

    private fun alert(
        id: String,
        type: AlertType,
        date: Long,
        headline: String,
        message: String,
        haltTrading: Boolean = false,
        requireVersionForTrading: Boolean = false,
        minVersion: String? = null,
    ): AuthorizedAlertData =
        AuthorizedAlertData(
            id = id,
            type = type,
            headline = headline,
            message = message,
            haltTrading = haltTrading,
            requireVersionForTrading = requireVersionForTrading,
            minVersion = minVersion,
            date = date,
        )

    private class MutableAlertNotificationsServiceFacade(
        private val alertsFlow: MutableStateFlow<List<AuthorizedAlertData>>,
    ) : AlertNotificationsServiceFacade() {
        var lastDismissedAlertId: String? = null

        override val alerts: StateFlow<List<AuthorizedAlertData>> = alertsFlow.asStateFlow()

        override fun dismissAlert(alertId: String) {
            lastDismissedAlertId = alertId
            alertsFlow.value = alertsFlow.value.filterNot { it.id == alertId }
        }
    }
}
