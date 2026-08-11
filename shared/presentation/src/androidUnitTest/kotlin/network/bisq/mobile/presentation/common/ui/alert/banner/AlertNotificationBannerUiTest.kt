package network.bisq.mobile.presentation.common.ui.alert.banner

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.MutableAlertNotificationsServiceFacade
import network.bisq.mobile.presentation.common.test_utils.authorizedAlert
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.test.presentation.compose.PlatformPresentationKoinComposeTestBase
import org.junit.Test
import kotlin.test.assertEquals

class AlertNotificationBannerUiTest : PlatformPresentationKoinComposeTestBase() {
    @Test
    fun `warn banner renders pending count and supports expand plus dismiss`() {
        val alertFacade =
            MutableAlertNotificationsServiceFacade(
                listOf(
                    authorizedAlert(
                        id = "warn",
                        type = AlertType.WARN,
                        date = 5L,
                        headline = "Security warning",
                        message = "Update soon",
                    ),
                    authorizedAlert(
                        id = "info",
                        type = AlertType.INFO,
                        date = 1L,
                        headline = "Info",
                        message = "FYI",
                    ),
                ),
            )
        val mainPresenter = MainPresenterTestFactory.create()
        mainPresenter.setIsMainContentVisible(true)
        val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade, FakeAppUpdateLinker())

        setTestContent {
            AlertNotificationBanner(presenter = presenter)
        }

        composeTestRule.onNodeWithText("Security warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Update soon").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.alert.pending.more".i18n(1)).assertIsDisplayed()

        composeTestRule
            .onNode(hasClickAction() and hasAnyDescendant(hasText("Security warning")), useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals(
                "warn",
                presenter.uiState.value.currentAlertDialog
                    ?.id,
            )
        }

        composeTestRule.onNodeWithContentDescription("action.close".i18n()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals("warn", alertFacade.lastDismissedAlertId)
            assertEquals(listOf("info"), alertFacade.alerts.value.map(AuthorizedAlertData::id))
        }
    }

    @Test
    fun `emergency banner shows trade halt and is not dismissible`() {
        val alertFacade =
            MutableAlertNotificationsServiceFacade(
                listOf(
                    authorizedAlert(
                        id = "emergency",
                        type = AlertType.EMERGENCY,
                        date = 10L,
                        headline = "Trading halted",
                        message = "Critical security alert",
                        haltTrading = true,
                    ),
                ),
            )
        val mainPresenter = MainPresenterTestFactory.create()
        mainPresenter.setIsMainContentVisible(true)
        val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade, FakeAppUpdateLinker())

        setTestContent {
            AlertNotificationBanner(presenter = presenter)
        }

        composeTestRule.onNodeWithText("Trading halted").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.alert.trade.halt".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("action.close".i18n()).assertCountEquals(0)
    }
}
