package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.SnackbarPosition
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals

/**
 * UI tests for [WebLinkConfirmationDialog] using Robolectric.
 *
 * These tests verify that the dialog composable renders and behaves correctly when settings and
 * the main presenter are provided via Koin, including persistence, clipboard and snackbar side
 * effects, auto-handling when confirmation is suppressed, and user interactions on the dialog.
 */
@RunWith(AndroidJUnit4::class)
class WebLinkDialogUiKoinTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    private fun setTestContent(
        uriHandler: UriHandler,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            KoinTestHost(uriHandler, content)
        }
    }

    /**
     * showWebLinkConfirmation = false,
     * permitOpeningBrowser = true,
     */
    @Test
    fun `when web link confirmation suppressed and browser permitted then opens uri invokes onConfirm without showing dialog`() {
        // Given
        val link = "https://example.com/auto"
        val uriHandler = WebLinkDialogCapturingUriHandler()
        val onConfirm = mockk<() -> Unit>(relaxed = true)
        startKoinWithWebLinkDeps(
            showWebLinkConfirmation = false,
            permitOpeningBrowser = true,
        )

        // When
        setTestContent(uriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = onConfirm,
                onDismiss = {},
                headline = "Should not appear",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Then
        composeTestRule.waitForIdle()
        assertEquals(listOf(link), uriHandler.openedUris)
        verify(exactly = 1) { onConfirm() }
        composeTestRule.assertNoNodeWithText("Should not appear")
    }

    /**
     * showWebLinkConfirmation = false,
     * permitOpeningBrowser = false,
     */
    @Test
    fun `when web link confirmation suppressed and browser not permitted then copies link shows snackbar invokes onDismiss without showing dialog`() {
        // Given
        val link = "https://example.com/copy-path"
        val onDismiss = mockk<() -> Unit>(relaxed = true)
        val (_, presenter) =
            startKoinWithWebLinkDeps(
                showWebLinkConfirmation = false,
                permitOpeningBrowser = false,
            )

        // When
        setTestContent(WebLinkDialogTestFixtures.noopUriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = onDismiss,
                headline = "Should not appear",
                headlineLeftIcon = null,
                message = "Hidden",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Then
        composeTestRule.waitForIdle()
        verify(exactly = 1) { onDismiss() }
        assertEquals(link, clipboardPrimaryText())
        verify(atLeast = 1) {
            presenter.showSnackbar(
                "mobile.components.copyIconButton.copied".i18n(),
                SnackbarType.SUCCESS,
                SnackbarPosition.BOTTOM,
                SnackbarDuration.Short,
            )
        }
        composeTestRule.assertNoNodeWithText("Should not appear")
    }

    /**
     * showWebLinkConfirmation = false,
     * permitOpeningBrowser = false,
     */
    @Test
    fun `when dismiss clicked then persists browser permit=false, copies link and invokes onDismiss`() {
        // Given
        val link = "https://example.com/no"
        val onDismiss = mockk<() -> Unit>(relaxed = true)
        val (facade, _) = startKoinWithWebLinkDeps()

        // When
        setTestContent(WebLinkDialogTestFixtures.noopUriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = onDismiss,
                headline = "Headline",
                headlineLeftIcon = null,
                message = "Message",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.waitForIdle()

        // Then
        coVerify(exactly = 1) { facade.setPermitOpeningBrowser(false) }
        coVerify(exactly = 0) { facade.setWebLinkDontShowAgain() }
        verify(exactly = 1) { onDismiss() }
        assertEquals(link, clipboardPrimaryText())
    }

    @Test
    fun `when dismiss clicked with dont show again checked then persists browser permit false, persists dont show flag and invokes onDismiss`() {
        // Given
        val link = "https://example.com/no-dsa"
        val onDismiss = mockk<() -> Unit>(relaxed = true)
        val (facade, _) = startKoinWithWebLinkDeps()

        // When
        setTestContent(WebLinkDialogTestFixtures.noopUriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = onDismiss,
                headline = "Headline",
                headlineLeftIcon = null,
                message = "Message",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).performClick()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.waitForIdle()

        // Then
        coVerify(exactly = 1) { facade.setPermitOpeningBrowser(false) }
        coVerify(exactly = 1) { facade.setWebLinkDontShowAgain() }
        verify(exactly = 1) { onDismiss() }
    }

    @Test
    fun `when set permit opening browser fails on dismiss then shows error, snackbar still shown, link copied and invokes onDismiss`() {
        // Given
        val link = "https://example.com/fail-dismiss"
        val onDismiss = mockk<() -> Unit>(relaxed = true)
        val (facade, presenter) =
            startKoinWithWebLinkDeps(
                setPermitResult = Result.failure(RuntimeException("network")),
            )

        // When
        setTestContent(WebLinkDialogTestFixtures.noopUriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = {},
                onDismiss = onDismiss,
                headline = "Headline",
                headlineLeftIcon = null,
                message = "Message",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.waitForIdle()

        // Then
        coVerify(exactly = 1) { facade.setPermitOpeningBrowser(false) } // but this errors
        verify(atLeast = 1) {
            presenter.showSnackbar(
                "mobile.error.generic".i18n(),
                SnackbarType.ERROR,
                SnackbarPosition.BOTTOM,
                SnackbarDuration.Short,
            )
        }
        verify(exactly = 1) { onDismiss() }
        assertEquals(link, clipboardPrimaryText())
    }

    @Test
    fun `when confirm clicked then persists browser permitted true, opens uri, does not set dont show again and invokes onConfirm`() {
        // Given
        val link = "https://example.com/yes"
        val uriHandler = WebLinkDialogCapturingUriHandler()
        val onConfirm = mockk<() -> Unit>(relaxed = true)
        val (facade, _) = startKoinWithWebLinkDeps()

        // When
        setTestContent(uriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = onConfirm,
                onDismiss = {},
                headline = "Headline",
                headlineLeftIcon = null,
                message = "Message",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        // Then
        coVerify(exactly = 1) { facade.setPermitOpeningBrowser(true) }
        coVerify(exactly = 0) { facade.setWebLinkDontShowAgain() }
        assertEquals(listOf(link), uriHandler.openedUris)
        verify(exactly = 1) { onConfirm() }
    }

    @Test
    fun `when confirm clicked with dont show again checked then persists browser permitted, persists dont show flag, opens uri and invokes onConfirm`() {
        // Given
        val link = "https://example.com/yes-dsa"
        val uriHandler = WebLinkDialogCapturingUriHandler()
        val onConfirm = mockk<() -> Unit>(relaxed = true)
        val (facade, _) = startKoinWithWebLinkDeps()

        // When
        setTestContent(uriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = onConfirm,
                onDismiss = {},
                headline = "Headline",
                headlineLeftIcon = null,
                message = "Message",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).performClick()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        // Then
        coVerify(exactly = 1) { facade.setPermitOpeningBrowser(true) }
        coVerify(exactly = 1) { facade.setWebLinkDontShowAgain() }
        assertEquals(listOf(link), uriHandler.openedUris)
        verify(exactly = 1) { onConfirm() }
    }

    @Test
    fun `when set permit opening browser fails on confirm then shows error snackbar still, opens uri and invokes onConfirm`() {
        // Given
        val link = "https://example.com/fail-confirm"
        val uriHandler = WebLinkDialogCapturingUriHandler()
        val onConfirm = mockk<() -> Unit>(relaxed = true)
        val (facade, presenter) =
            startKoinWithWebLinkDeps(
                setPermitResult = Result.failure(RuntimeException("network")),
            )

        // When
        setTestContent(uriHandler) {
            WebLinkConfirmationDialog(
                link = link,
                onConfirm = onConfirm,
                onDismiss = {},
                headline = "Headline",
                headlineLeftIcon = null,
                message = "Message",
                confirmButtonText = "Yes",
                dismissButtonText = "No",
            )
        }

        // Action
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        // Then
        coVerify(exactly = 1) { facade.setPermitOpeningBrowser(true) } // but this errors
        verify(atLeast = 1) {
            presenter.showSnackbar(
                "mobile.error.generic".i18n(),
                SnackbarType.ERROR,
                SnackbarPosition.BOTTOM,
                SnackbarDuration.Short,
            )
        }
        assertEquals(listOf(link), uriHandler.openedUris)
        verify(exactly = 1) { onConfirm() }
    }

    /**
     * Flow test:
     * 1. Open dialog with link1
     * 2. Check don't show again and open link
     * 3. Open dialog with link2
     * 4. Should skip dialog render and open link
     */
    @Test
    fun `when dont show again after open url then second link opens uri without dialog`() {
        // Given
        val link1 = "https://example.com/twostep-open-first"
        val link2 = "https://example.com/twostep-open-second"
        val headline = "Headline"
        val message = "Message"
        val confirmButtonText = "Yes"
        val dismissButtonText = "No"
        val fake = WebLinkDialogSettingsServiceFake()
        val uriHandler = WebLinkDialogCapturingUriHandler()
        val onConfirm = mockk<() -> Unit>(relaxed = true)
        startKoinWithWebLinkDialogFake(fake)

        // One setContent per test (Compose rule); drive second navigation by changing link state.
        val linkState = mutableStateOf(link1)
        composeTestRule.setContent {
            KoinTestHost(uriHandler) {
                key(linkState.value) {
                    WebLinkConfirmationDialog(
                        link = linkState.value,
                        onConfirm = onConfirm,
                        onDismiss = {},
                        headline = headline,
                        headlineLeftIcon = null,
                        message = message,
                        confirmButtonText = confirmButtonText,
                        dismissButtonText = dismissButtonText,
                    )
                }
            }
        }

        // When — first link: full dialog, dont show again + confirm
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).performClick()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        // Then — persisted state matches production semantics
        assertEquals(false, fake.snapshotShowWebLinkConfirmation())
        assertEquals(true, fake.snapshotPermitOpeningBrowser())
        assertEquals(listOf(link1), uriHandler.openedUris)
        verify(exactly = 1) { onConfirm() }

        // When — second link: suppressed confirmation, auto-open
        composeTestRule.runOnIdle { linkState.value = link2 }
        composeTestRule.waitForIdle()

        // Then
        assertEquals(listOf(link1, link2), uriHandler.openedUris)
        verify(exactly = 2) { onConfirm() }
        composeTestRule.assertNoNodeWithText(headline)
    }

    /**
     * Flow test:
     * 1. Open dialog with link1
     * 2. Check don't show again and copy link
     * 3. Open dialog with link2
     * 4. Should skip dialog render and copy link and show snackbar
     */
    @Test
    fun `when dont show again after copy link then second link copies without dialog`() {
        // Given
        val link1 = "https://example.com/twostep-copy-first"
        val link2 = "https://example.com/twostep-copy-second"
        val headline = "Headline"
        val message = "Message"
        val confirmButtonText = "Yes"
        val dismissButtonText = "No"
        val fake = WebLinkDialogSettingsServiceFake()
        val onDismiss = mockk<() -> Unit>(relaxed = true)
        val (_, presenter) = startKoinWithWebLinkDialogFake(fake)

        val linkState = mutableStateOf(link1)
        composeTestRule.setContent {
            KoinTestHost(WebLinkDialogTestFixtures.noopUriHandler) {
                key(linkState.value) {
                    WebLinkConfirmationDialog(
                        link = linkState.value,
                        onConfirm = {},
                        onDismiss = onDismiss,
                        headline = headline,
                        headlineLeftIcon = null,
                        message = message,
                        confirmButtonText = confirmButtonText,
                        dismissButtonText = dismissButtonText,
                    )
                }
            }
        }

        // When — first link: full dialog, dont show again + dismiss (no)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).performClick()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.waitForIdle()

        // Then
        assertEquals(false, fake.snapshotShowWebLinkConfirmation())
        assertEquals(false, fake.snapshotPermitOpeningBrowser())
        assertEquals(link1, clipboardPrimaryText())
        verify(exactly = 1) { onDismiss() }
        verify(atLeast = 1) {
            presenter.showSnackbar(
                "mobile.components.copyIconButton.copied".i18n(),
                SnackbarType.SUCCESS,
                SnackbarPosition.BOTTOM,
                SnackbarDuration.Short,
            )
        }

        // When — second link: suppressed confirmation, auto-copy
        composeTestRule.runOnIdle { linkState.value = link2 }
        composeTestRule.waitForIdle()

        // Then
        assertEquals(link2, clipboardPrimaryText())
        verify(exactly = 2) { onDismiss() }
        verify(atLeast = 2) {
            presenter.showSnackbar(
                "mobile.components.copyIconButton.copied".i18n(),
                SnackbarType.SUCCESS,
                SnackbarPosition.BOTTOM,
                SnackbarDuration.Short,
            )
        }
        composeTestRule.assertNoNodeWithText(headline)
    }
}
