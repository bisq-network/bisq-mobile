package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WelcomeCarouselContentUiTest : BisqComposeUiTestBase() {
    private fun setTestContent(
        uiState: WelcomeCarouselUiState,
        onPrimaryAction: (CarouselPageType) -> Unit = {},
        onAction: (WelcomeCarouselUiAction) -> Unit = {},
    ) {
        setTestContent {
            WelcomeCarouselContent(
                uiState = uiState,
                onPrimaryAction = onPrimaryAction,
                onAction = onAction,
            )
        }
    }

    @Test
    fun `when single NOTIFICATIONS page then notification title and action shown`() {
        setTestContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.NOTIFICATIONS),
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.notifications.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.notifications.action".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.dontAskAgain".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when single BATTERY page then battery title and action shown`() {
        setTestContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.BATTERY),
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.battery.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.battery.action".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when both pages then first page renders and other page title not visible`() {
        setTestContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.NOTIFICATIONS, CarouselPageType.BATTERY),
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.notifications.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("mobile.welcomeCarousel.battery.title".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `tapping primary action invokes callback with current page type`() {
        var capturedType: CarouselPageType? = null
        setTestContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.NOTIFICATIONS),
                ),
            onPrimaryAction = { capturedType = it },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.notifications.action".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(CarouselPageType.NOTIFICATIONS, capturedType)
    }

    @Test
    fun `tapping Don't ask again dispatches OnDontAskAgain for current page type`() {
        var capturedAction: WelcomeCarouselUiAction? = null
        setTestContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.BATTERY),
                ),
            onAction = { capturedAction = it },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.dontAskAgain".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            WelcomeCarouselUiAction.OnDontAskAgain(CarouselPageType.BATTERY),
            capturedAction,
        )
    }

    @Test
    fun `empty pages renders nothing observable`() {
        var capturedType: CarouselPageType? = null
        var capturedAction: WelcomeCarouselUiAction? = null
        setTestContent(
            uiState = WelcomeCarouselUiState(pages = emptyList()),
            onPrimaryAction = { capturedType = it },
            onAction = { capturedAction = it },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("mobile.welcomeCarousel.notifications.title".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("mobile.welcomeCarousel.battery.title".i18n())
            .assertCountEquals(0)
        assertNull(capturedType)
        assertNull(capturedAction)
    }
}
