package network.bisq.mobile.presentation.common.ui.alert.banner

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AlertNotificationBannerUiTest {
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
    fun `warn banner renders pending count and supports expand plus dismiss`() =
        runTest(testDispatcher) {
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(id = "warn", type = AlertType.WARN, date = 5L, headline = "Security warning", message = "Update soon"),
                        alert(id = "info", type = AlertType.INFO, date = 1L, headline = "Info", message = "FYI"),
                    ),
                )
            val alertFacade = MutableAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            mainPresenter.setIsMainContentVisible(true)
            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade)

            composeTestRule.setContent {
                CompositionLocalProvider(LocalIsTest provides true) {
                    BisqTheme {
                        AlertNotificationBanner(presenter = presenter)
                    }
                }
            }

            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Security warning").assertIsDisplayed()
            composeTestRule.onNodeWithText("Update soon").assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.alert.pending.more".i18n(1)).assertIsDisplayed()

            composeTestRule
                .onNode(hasClickAction() and hasAnyDescendant(hasText("Security warning")), useUnmergedTree = true)
                .performClick()
            advanceUntilIdle()
            composeTestRule.runOnIdle {
                assertEquals(
                    "warn",
                    presenter.uiState.value.currentAlertDialog
                        ?.id,
                )
            }

            composeTestRule.onNodeWithContentDescription("action.close".i18n()).performClick()
            advanceUntilIdle()
            composeTestRule.runOnIdle {
                assertEquals("warn", alertFacade.lastDismissedAlertId)
                assertEquals(listOf("info"), alertFacade.alerts.value.map(AuthorizedAlertData::id))
            }
        }

    @Test
    fun `emergency banner shows trade halt and is not dismissible`() =
        runTest(testDispatcher) {
            val alertsFlow =
                MutableStateFlow(
                    listOf(
                        alert(
                            id = "emergency",
                            type = AlertType.EMERGENCY,
                            date = 10L,
                            headline = "Trading halted",
                            message = "Critical security alert",
                            haltTrading = true,
                        ),
                    ),
                )
            val alertFacade = MutableAlertNotificationsServiceFacade(alertsFlow)
            val mainPresenter = MainPresenterTestFactory.create()
            mainPresenter.setIsMainContentVisible(true)
            val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade)

            composeTestRule.setContent {
                CompositionLocalProvider(LocalIsTest provides true) {
                    BisqTheme {
                        AlertNotificationBanner(presenter = presenter)
                    }
                }
            }

            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Trading halted").assertIsDisplayed()
            composeTestRule.onNodeWithText("mobile.alert.trade.halt".i18n()).assertIsDisplayed()
            composeTestRule.onAllNodesWithContentDescription("action.close".i18n()).assertCountEquals(0)
        }

    private fun alert(
        id: String,
        type: AlertType,
        date: Long,
        headline: String,
        message: String,
        haltTrading: Boolean = false,
    ): AuthorizedAlertData =
        AuthorizedAlertData(
            id = id,
            type = type,
            headline = headline,
            message = message,
            haltTrading = haltTrading,
            requireVersionForTrading = false,
            minVersion = null,
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
