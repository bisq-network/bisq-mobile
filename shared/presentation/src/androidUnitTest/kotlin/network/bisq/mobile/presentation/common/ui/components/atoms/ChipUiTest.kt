package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [BisqChip], focused on the survey-chip additions: the selected state, the
 * Compact size, and the removable trailing icon being truly absent (not a dead slot) when
 * `showRemove` is off.
 */
class ChipUiTest : BisqComposeUiTestBase() {
    private val label = "Price moved"

    @Test
    fun `when chip tapped then invokes onClick with label`() {
        val onClick = mockk<(String) -> Unit>(relaxed = true)

        setTestContent {
            // No remove icon: with it shown, a center tap can land on the trailing IconButton
            // instead of the chip surface — the survey chips render exactly this shape.
            BisqChip(label = label, showRemove = false, onClick = onClick)
        }

        composeTestRule.onNodeWithText(label).performClick()

        verify(exactly = 1) { onClick(label) }
    }

    @Test
    fun `when remove shown and tapped then invokes onRemove with label`() {
        val onRemove = mockk<(String) -> Unit>(relaxed = true)

        setTestContent {
            BisqChip(label = label, showRemove = true, onRemove = onRemove)
        }

        composeTestRule.onNodeWithContentDescription("close").performClick()

        verify(exactly = 1) { onRemove(label) }
    }

    @Test
    fun `when remove hidden then close icon is absent`() {
        setTestContent {
            BisqChip(label = label, showRemove = false)
        }

        composeTestRule.onNodeWithText(label).assertExists()
        composeTestRule.onNodeWithContentDescription("close").assertDoesNotExist()
    }

    @Test
    fun `when compact selected default chip then renders label`() {
        setTestContent {
            BisqChip(
                label = label,
                showRemove = false,
                selected = true,
                size = BisqChipSize.Compact,
            )
        }

        composeTestRule.onNodeWithText(label).assertExists()
    }

    @Test
    fun `when outline selected chip then renders label`() {
        setTestContent {
            BisqChip(
                label = label,
                showRemove = false,
                selected = true,
                type = BisqChipType.Outline,
            )
        }

        composeTestRule.onNodeWithText(label).assertExists()
    }
}
