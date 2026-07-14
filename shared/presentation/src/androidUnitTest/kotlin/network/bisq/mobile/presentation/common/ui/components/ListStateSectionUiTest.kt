package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListStateSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme { content() }
            }
        }
    }

    @Test
    fun `full state - icon, headline title, subtitle and button render, and button dispatches`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setContent {
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
        setContent {
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
        setContent { ListStateSection(title = "Nothing here") }
        composeTestRule.onNodeWithText("Nothing here").assertIsDisplayed()
    }
}
