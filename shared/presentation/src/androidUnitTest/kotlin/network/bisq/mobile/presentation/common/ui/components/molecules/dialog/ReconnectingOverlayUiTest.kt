package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * Regression for #1615: on short viewports (e.g. iPhone X) the reconnect action must stay
 * visible while long details text scrolls.
 */
class ReconnectingOverlayUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when viewport is short then restart services button stays displayed`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        val buttonText = "mobile.connectivity.reconnecting.restartServices".i18n()

        setTestContent {
            // Roughly an iPhone X content height after chrome/safe-area, narrow enough that
            // the long iOS Connect details copy would push a non-sticky button off-screen.
            Box(modifier = Modifier.size(width = 320.dp, height = 480.dp)) {
                ReconnectingOverlay(
                    onClick = onClick,
                    infoKey = "mobile.connectivity.reconnecting.client.info",
                    detailsKey = "mobile.connectivity.reconnecting.client.details.ios",
                    buttonTextKey = "mobile.connectivity.reconnecting.restartServices",
                )
            }
        }

        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()
        composeTestRule.onNodeWithText(buttonText).performClick()
        verify(exactly = 1) { onClick() }
    }

    @Test
    fun `when content fits then title and button are displayed`() {
        setTestContent {
            ReconnectingOverlay()
        }

        composeTestRule
            .onNodeWithText("mobile.connectivity.reconnecting.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.connectivity.reconnecting.restart".i18n())
            .assertIsDisplayed()
    }
}
