package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteTextUiSmokeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders note text with link`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides NoopUriHandler()) {
                BisqTheme {
                    NoteText(
                        notes = "Read docs",
                        linkText = "Open link",
                        uri = "https://example.com",
                        openConfirmation = false,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Read docs Open link").assertIsDisplayed()
    }

    private class NoopUriHandler : UriHandler {
        override fun openUri(uri: String) {}
    }
}
