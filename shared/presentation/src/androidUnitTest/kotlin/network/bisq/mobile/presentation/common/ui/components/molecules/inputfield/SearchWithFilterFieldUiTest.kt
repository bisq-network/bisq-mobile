package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class SearchWithFilterFieldUiTest : BisqComposeUiTestBase() {
    @Test
    fun `inactive filter renders the search field with its placeholder`() {
        setTestContent {
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
        setTestContent {
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
