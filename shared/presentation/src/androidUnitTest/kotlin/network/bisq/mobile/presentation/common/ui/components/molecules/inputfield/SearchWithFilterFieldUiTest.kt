package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchWithFilterFieldUiTest {
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
    fun `inactive filter renders the search field with its placeholder`() {
        setContent {
            SearchWithFilterField(
                value = "",
                onValueChange = {},
                isFilterActive = false,
                onFilterClick = {},
                placeholder = "Search here",
            )
        }
        composeTestRule.onNodeWithText("Search here").assertIsDisplayed()
    }

    @Test
    fun `active filter renders the field with its current value (green icon branch)`() {
        setContent {
            SearchWithFilterField(
                value = "bitcoin",
                onValueChange = {},
                isFilterActive = true,
                onFilterClick = {},
                placeholder = "Search here",
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("bitcoin").assertIsDisplayed()
    }
}
