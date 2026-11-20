package network.bisq.mobile.presentation.common.ui.components.atoms.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertTrue

/**
 * UI tests for [InlineOrStacked]: one line while everything fits, otherwise the trailing content
 * below the leading one, never squeezed below the width it needs.
 *
 * Fixed-size boxes stand in for text: Robolectric's text widths are not a device's, while a box's
 * intrinsic width is exactly its size, so each case is decided by the numbers in the test.
 */
class InlineOrStackedUiTest : BisqComposeUiTestBase() {
    @Test
    fun `content that fits stays on one line with the separator`() {
        render(width = 200.dp, leadingWidth = 100.dp, trailingWidth = 60.dp)

        assertTrue(bounds(TRAILING).left >= bounds(SEPARATOR).right, "trailing should follow the separator")
        assertTrue(bounds(TRAILING).top < bounds(LEADING).bottom, "trailing should share the leading line")
    }

    @Test
    fun `content that does not fit stacks and leaves the separator out`() {
        render(width = 150.dp, leadingWidth = 100.dp, trailingWidth = 60.dp)

        assertTrue(bounds(TRAILING).top >= bounds(LEADING).bottom, "trailing should sit below leading")
        composeTestRule.onNodeWithTag(SEPARATOR).assertIsNotDisplayed()
    }

    @Test
    fun `a stacked trailing keeps its full width instead of being squeezed`() {
        render(width = 150.dp, leadingWidth = 100.dp, trailingWidth = 60.dp)

        composeTestRule.onNodeWithTag(TRAILING).assertWidthIsEqualTo(60.dp)
    }

    @Test
    fun `a trailing minimum keeps a wide trailing inline while that minimum fits`() {
        // 100 + 10 separator + 48 minimum = 158dp fits in 170dp, although the 200dp trailing does not.
        render(width = 170.dp, leadingWidth = 100.dp, trailingWidth = 200.dp, trailingMinWidth = 48.dp)

        assertTrue(bounds(TRAILING).top < bounds(LEADING).bottom, "trailing should stay on the leading line")
    }

    @Test
    fun `a longer leading such as a translated label pushes the trailing below it`() {
        // The same row with the leading 40% longer, as "Sell Bitcoin to" is in German.
        render(width = 170.dp, leadingWidth = 140.dp, trailingWidth = 200.dp, trailingMinWidth = 48.dp)

        assertTrue(bounds(TRAILING).top >= bounds(LEADING).bottom, "trailing should sit below leading")
    }

    @Test
    fun `an intrinsic-height parent is exactly as tall as the inline layout`() {
        renderInIntrinsicHeightParent(width = 200.dp)

        composeTestRule.onNodeWithTag(PARENT).assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun `an intrinsic-height parent is exactly as tall as the stacked layout`() {
        renderInIntrinsicHeightParent(width = 150.dp)

        // 20dp leading + 4dp spacing + 20dp trailing.
        composeTestRule.onNodeWithTag(PARENT).assertHeightIsEqualTo(44.dp)
    }

    private fun renderInIntrinsicHeightParent(width: Dp) {
        setTestContent {
            Row(modifier = Modifier.width(width).height(IntrinsicSize.Min).testTag(PARENT)) {
                InlineOrStacked(
                    leading = { Box(modifier = Modifier.size(100.dp, 20.dp)) },
                    separator = { Box(modifier = Modifier.size(10.dp, 20.dp)) },
                    trailing = { Box(modifier = Modifier.size(60.dp, 20.dp)) },
                    stackedSpacing = 4.dp,
                )
            }
        }
    }

    private fun render(
        width: Dp,
        leadingWidth: Dp,
        trailingWidth: Dp,
        trailingMinWidth: Dp? = null,
    ) {
        setTestContent {
            Box(modifier = Modifier.width(width)) {
                InlineOrStacked(
                    leading = { Box(modifier = Modifier.size(leadingWidth, 20.dp).testTag(LEADING)) },
                    separator = { Box(modifier = Modifier.size(10.dp, 20.dp).testTag(SEPARATOR)) },
                    trailing = { Box(modifier = Modifier.size(trailingWidth, 20.dp).testTag(TRAILING)) },
                    trailingMinWidth = trailingMinWidth,
                )
            }
        }
    }

    private fun bounds(tag: String) = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()

    private companion object {
        const val LEADING = "leading"
        const val SEPARATOR = "separator"
        const val TRAILING = "trailing"
        const val PARENT = "parent"
    }
}
