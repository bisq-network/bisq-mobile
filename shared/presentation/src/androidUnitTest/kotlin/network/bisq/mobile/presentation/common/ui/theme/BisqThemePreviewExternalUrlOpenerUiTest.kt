package network.bisq.mobile.presentation.common.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BisqThemePreviewExternalUrlOpenerUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

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
    val opened = LocalExternalUrlOpener.current.openUrl("https://example.com")
    Text(text = "opened:$opened")
}
