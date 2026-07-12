package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.compose.PresentationKoinComposeTestBase
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import org.koin.core.module.Module

/**
 * UI tests for [WebLinkConfirmationDialog] using Robolectric.
 *
 * These tests verify that the dialog composable renders correctly for default and custom strings
 * and that user interactions trigger the appropriate callbacks. Settings and the main presenter
 * are provided via [additionalModules] / [onKoinReady], matching [PresentationKoinComposeTestBase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebLinkDialogUiIsolatedTest : PresentationKoinComposeTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var settingsFacade: WebLinkDialogSettingsServiceFake

    override fun additionalModules(): List<Module> = listOf(webLinkConfirmationTestModule({ mainPresenter }, { settingsFacade }))

    override fun onKoinReady() {
        settingsFacade = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        mainPresenter = mockk(relaxed = true)
        coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns true
    }

    private fun setIsolatedTestContent(content: @Composable () -> Unit) {
        setTestContent {
            CompositionLocalProvider(
                LocalExternalUrlOpener provides WebLinkDialogTestFixtures.noopExternalUrlOpener,
            ) {
                content()
            }
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
        setIsolatedTestContent {
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
        setIsolatedTestContent {
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
        setIsolatedTestContent {
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
        setIsolatedTestContent {
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
