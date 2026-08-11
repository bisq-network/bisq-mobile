package network.bisq.mobile.presentation.common.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class BisqThemePreviewExternalUrlOpenerUiTest : BisqComposeUiTestBase() {
    @Test
    fun `BisqTheme Preview provides noop LocalExternalUrlOpener`() {
        composeTestRule.setContent {
            BisqTheme.Preview {
                PreviewConsumer()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("opened:false").assertExists()
    }
}

@Composable
private fun PreviewConsumer() {
    val opener = LocalExternalUrlOpener.current
    var opened by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(opener) {
        opened = opener.openUrl("https://example.com")
    }
    Text(text = "opened:${opened ?: "pending"}")
}
