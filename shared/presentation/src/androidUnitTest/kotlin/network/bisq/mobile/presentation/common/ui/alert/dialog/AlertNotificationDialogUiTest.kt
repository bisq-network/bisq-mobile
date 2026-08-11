package network.bisq.mobile.presentation.common.ui.alert.dialog

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.utils.AppUpdateLinker
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.MutableAlertNotificationsServiceFacade
import network.bisq.mobile.presentation.common.test_utils.TEST_APP_UPDATE_URL
import network.bisq.mobile.presentation.common.test_utils.authorizedAlert
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.test.presentation.compose.PlatformPresentationKoinComposeTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlertNotificationDialogUiTest : PlatformPresentationKoinComposeTestBase() {
    @Test
    fun `update dialog renders version details and supports update plus dismiss`() {
        val appUpdateLinker = mockk<AppUpdateLinker>()
        every { appUpdateLinker.getUpdateUrl() } returns TEST_APP_UPDATE_URL
        val urlLauncher = mockk<UrlLauncher>(relaxed = true)
        coEvery { urlLauncher.openUrl(any()) } returns true
        val alertFacade =
            MutableAlertNotificationsServiceFacade(
                listOf(
                    authorizedAlert(
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
        val mainPresenter = MainPresenterTestFactory.create(urlLauncher = urlLauncher)
        val presenter =
            AlertNotificationBannerPresenter(
                mainPresenter,
                alertFacade,
                appUpdateLinker,
            )

        presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("update"))

        setTestContent {
            AlertNotificationDialog(presenter = presenter)
        }

        composeTestRule.onNodeWithText("Update required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Install the patched build.").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.alert.update.minimum".i18n("2.1.8")).assertIsDisplayed()

        composeTestRule.onNodeWithText("mobile.alert.update.button".i18n()).performClick()
        composeTestRule.waitForIdle()
        coVerify(exactly = 1) { urlLauncher.openUrl(TEST_APP_UPDATE_URL) }
        verify(exactly = 1) { appUpdateLinker.getUpdateUrl() }

        composeTestRule
            .onNode(
                hasClickAction() and hasAnyDescendant(hasText("mobile.alert.actions.dismiss.label".i18n())),
                useUnmergedTree = true,
            ).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals("update", alertFacade.lastDismissedAlertId)
            assertNull(presenter.uiState.value.currentAlertDialog)
        }
    }

    @Test
    fun `halt trading dialog shows halt message and can be closed`() {
        val alertFacade =
            MutableAlertNotificationsServiceFacade(
                listOf(
                    authorizedAlert(
                        id = "halt",
                        type = AlertType.EMERGENCY,
                        date = 9L,
                        headline = "Trading suspended",
                        message = "Critical issue detected.",
                        haltTrading = true,
                    ),
                ),
            )
        val mainPresenter = MainPresenterTestFactory.create()
        val presenter = AlertNotificationBannerPresenter(mainPresenter, alertFacade, FakeAppUpdateLinker())

        presenter.onAction(AlertNotificationUiAction.ExpandAlertNotification("halt"))

        setTestContent {
            AlertNotificationDialog(presenter = presenter)
        }

        composeTestRule.onNodeWithText("mobile.alert.trade.halt".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("mobile.alert.update.button".i18n()).assertCountEquals(0)

        composeTestRule
            .onNodeWithContentDescription("mobile.alert.actions.dismiss.description".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertNull(presenter.uiState.value.currentAlertDialog)
        }
    }
}
