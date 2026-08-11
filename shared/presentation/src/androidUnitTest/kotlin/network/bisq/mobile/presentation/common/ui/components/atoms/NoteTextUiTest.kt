package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class NoteTextUiTest : BisqComposeUiTestBase() {
    @Test
    fun `renders note text with link`() {
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                NoteText(
                    notes = "Read docs",
                    linkText = "Open link",
                    uri = "https://example.com",
                    openConfirmation = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Read docs Open link").assertIsDisplayed()
    }
}
