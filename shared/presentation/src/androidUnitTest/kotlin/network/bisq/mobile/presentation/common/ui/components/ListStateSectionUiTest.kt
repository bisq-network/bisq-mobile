package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class ListStateSectionUiTest : BisqComposeUiTestBase() {
    @Test
    fun `full state - icon, headline title, subtitle and button render, and button dispatches`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setTestContent {
            ListStateSection(
                title = "No trades yet",
                subtitle = "Your completed trades will show up here",
                icon = { BisqText.BaseLight("ICON") },
                buttonText = "Browse offers",
                onButtonClick = onClick,
            )
        }
        composeTestRule.onNodeWithText("ICON").assertIsDisplayed()
        composeTestRule.onNodeWithText("No trades yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your completed trades will show up here").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browse offers").performClick()
        verify(exactly = 1) { onClick() }
    }

    @Test
    fun `minimal state - non-headline title, no icon, no subtitle, grey button renders`() {
        setTestContent {
            ListStateSection(
                title = "No results match your search",
                useHeadlineStyle = false,
                buttonText = "Clear search",
                buttonType = BisqButtonType.Grey,
            )
        }
        composeTestRule.onNodeWithText("No results match your search").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear search").assertIsDisplayed()
    }

    @Test
    fun `title only renders without icon, subtitle or button`() {
        setTestContent { ListStateSection(title = "Nothing here") }
        composeTestRule.onNodeWithText("Nothing here").assertIsDisplayed()
    }
}
