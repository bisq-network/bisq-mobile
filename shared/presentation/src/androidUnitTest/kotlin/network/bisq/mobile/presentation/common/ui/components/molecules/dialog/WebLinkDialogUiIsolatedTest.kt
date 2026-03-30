package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/**
 * UI tests for [WebLinkConfirmationDialog] using Robolectric.
 *
 * These tests verify that the dialog composable renders correctly for default and custom strings
 * and that user interactions trigger the appropriate callbacks. Koin is not used; test-only
 * composition locals supply fallbacks instead of production services.
 */
@RunWith(AndroidJUnit4::class)
class WebLinkDialogUiIsolatedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        startKoinWithWebLinkDeps()
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            IsolatedTestHost(content)
        }
    }

    @Test
    fun `when dialog renders with default strings`() {
        // Given
        val link = "https://example.com/default-link"

        // Default values
        val expectedHeadline = "hyperlinks.openInBrowser.attention.headline".i18n()
        val expectedMessage = "hyperlinks.openInBrowser.attention".i18n(link)
        val expectedConfirm = "confirmation.yes".i18n()
        val expectedDismiss = "hyperlinks.openInBrowser.no".i18n()
        val expectedDontShowAgain = "action.dontShowAgain".i18n()

        // When
        setTestContent {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = {},
            )
        }

        // Then - All controls with default values
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(expectedHeadline).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedConfirm).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedDismiss).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedDontShowAgain).assertIsDisplayed()
    }

    @Test
    fun `when dialog renders with custom strings`() {
        // Given
        val link = "https://example.com/path"
        val headline = "Open external link?"
        val message = "You are about to open $link"
        val confirmButtonText = "Open link"
        val dismissButtonText = "Not now"
        // Default
        val expectedDontShowAgain = "action.dontShowAgain".i18n()

        // When
        setTestContent {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = {},
                headline = headline,
                headlineLeftIcon = null,
                message = message,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
            )
        }

        // Then - All controls with given values
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(headline).assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithText(confirmButtonText).assertIsDisplayed()
        composeTestRule.onNodeWithText(dismissButtonText).assertIsDisplayed()

        // Then - All controls with default values
        composeTestRule.onNodeWithText(expectedDontShowAgain).assertIsDisplayed()
    }

    @Test
    fun `when close button clicked then invokes onDismiss`() {
        // Given
        val link = "https://example.com"
        val headline = "Headline"
        val message = "Message body"
        val confirmButtonText = "Yes"
        val dismissButtonText = "No"
        val closeContentDescription = "close"
        val onDismiss = mockk<() -> Unit>(relaxed = true)

        // When
        setTestContent {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = onDismiss,
                headline = headline,
                headlineLeftIcon = null,
                message = message,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(closeContentDescription).performClick()
        composeTestRule.waitForIdle()

        // Then
        verify(exactly = 1) { onDismiss() }
    }

    @Test
    fun `when confirm button clicked then invokes onConfirm`() {
        // Given
        val link = "https://example.com"
        val headline = "Headline"
        val message = "Message body"
        val confirmButtonText = "Yes"
        val dismissButtonText = "No"
        val confirmButtonContentDescription = "dialog_confirm_yes"
        val onConfirm = mockk<() -> Unit>(relaxed = true)

        // When
        setTestContent {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = onConfirm,
                onDismiss = {},
                headline = headline,
                headlineLeftIcon = null,
                message = message,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(confirmButtonContentDescription).performClick()
        composeTestRule.waitForIdle()

        // Then
        verify(exactly = 1) { onConfirm() }
    }
}
