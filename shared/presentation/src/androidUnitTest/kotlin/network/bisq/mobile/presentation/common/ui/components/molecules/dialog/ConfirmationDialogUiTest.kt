package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertNotEquals

/** UI test for [ConfirmationDialog]'s vertical button placement with both buttons present. */
class ConfirmationDialogUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when vertical placement with both buttons then both render and confirm fires`() {
        val onConfirm = mockk<() -> Unit>(relaxed = true)
        val onDismiss = mockk<(Boolean) -> Unit>(relaxed = true)

        setTestContent {
            ConfirmationDialog(
                headline = "Vertical",
                message = "Stacked buttons",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
                verticalButtonPlacement = true,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }

        // Actually stacked, not side by side: in the horizontal Row both buttons share a top edge.
        val yesTop = composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").getUnclippedBoundsInRoot().top
        val noTop = composeTestRule.onNodeWithContentDescription("dialog_confirm_no").getUnclippedBoundsInRoot().top
        assertNotEquals(yesTop, noTop)

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()

        verify(exactly = 1) { onConfirm() }
        verify(exactly = 0) { onDismiss(any()) }
    }
}
