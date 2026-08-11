package network.bisq.mobile.presentation.common.ui.components.atoms.slider

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * Regression tests for issue #1571: [BisqRangeSlider] must not crash when handed an inverted or
 * non-finite value range. Material3's RangeSlider runs the thumbs through coerceIn during measure
 * and throws IllegalArgumentException on inverted/NaN bounds; the wrapper sanitizes inputs so these
 * compose without throwing (before the fix, measuring these would crash the whole screen).
 */
class BisqRangeSliderUiTest : BisqComposeUiTestBase() {
    private val tag = "range_slider"

    private fun render(
        value: ClosedFloatingPointRange<Float>,
        valueRange: ClosedFloatingPointRange<Float>,
    ) {
        setTestContent {
            BisqRangeSlider(
                value = value,
                onValueChange = {},
                modifier = Modifier.testTag(tag),
                valueRange = valueRange,
            )
        }
    }

    @Test
    fun `inverted value range does not crash and still renders`() {
        // reputation-based max collapsing below 0 → valueRange = 0f..negative (the crash case)
        render(value = 0.1f..0.9f, valueRange = 0f..(-0.5f))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(tag).assertExists()
    }

    @Test
    fun `selection outside a shrunken range does not crash and still renders`() {
        render(value = 0.1f..0.9f, valueRange = 0f..0.05f)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(tag).assertExists()
    }

    @Test
    fun `non-finite bounds do not crash and still render`() {
        render(value = Float.NaN..Float.NaN, valueRange = 0f..Float.NaN)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(tag).assertExists()
    }

    @Test
    fun `valid range renders normally`() {
        render(value = 0.25f..0.75f, valueRange = 0f..1f)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(tag).assertExists()
    }
}
