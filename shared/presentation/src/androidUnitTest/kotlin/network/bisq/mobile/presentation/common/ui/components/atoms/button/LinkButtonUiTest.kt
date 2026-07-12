package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.compose.PresentationKoinComposeTestBase
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkDialogSettingsServiceFake
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.mockNavigateToUrlBehavior
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.webLinkConfirmationTestModule
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import org.koin.core.module.Module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LinkButtonUiTest : PresentationKoinComposeTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var settingsFacade: WebLinkDialogSettingsServiceFake

    private val dialogTitle get() = "hyperlinks.openInBrowser.attention.headline".i18n()

    override fun additionalModules(): List<Module> = listOf(webLinkConfirmationTestModule({ mainPresenter }, { settingsFacade }))

    override fun onKoinReady() {
        settingsFacade = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        mainPresenter = mockk(relaxed = true)
        coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns true
    }

    private fun setLinkButton(
        externalUrlOpener: ExternalUrlOpener,
        text: String = "Open docs",
        link: String = "https://example.com",
        onClick: (() -> Unit)? = null,
        openConfirmation: Boolean = true,
        forceConfirm: Boolean = false,
    ) {
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides externalUrlOpener) {
                LinkButton(
                    text = text,
                    link = link,
                    onClick = onClick,
                    openConfirmation = openConfirmation,
                    forceConfirm = forceConfirm,
                )
            }
        }
    }

    @Test
    fun `when clicked with openConfirmation true then shows confirmation dialog`() {
        setLinkButton(externalUrlOpener = noopExternalOpener())

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when forceConfirm true and showWebLinkConfirmation false then shows confirmation dialog`() {
        settingsFacade.setShowWebLinkConfirmation(false)
        setLinkButton(externalUrlOpener = noopExternalOpener(), forceConfirm = true)

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        assertNoNodeWithText("action.dontShowAgain".i18n())
    }

    @Test
    fun `when clicked with openConfirmation false then invokes onClick without dialog`() {
        val capturing = CapturingExternalUrlOpener()
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            externalUrlOpener = capturing,
            onClick = onClick,
            openConfirmation = false,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onClick() }
        assertNoNodeWithText(dialogTitle)
        assertEquals(listOf("https://example.com"), capturing.openedUrls)
    }

    @Test
    fun `when openConfirmation false and openUrl returns false then does not invoke onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            externalUrlOpener = ExternalUrlOpener { false },
            link = "https://example.com/rejected",
            onClick = onClick,
            openConfirmation = false,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 0) { onClick() }
    }

    @Test
    fun `when openConfirmation false and link is blank then invokes onClick without opening uri`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        val capturing = CapturingExternalUrlOpener()
        setLinkButton(
            externalUrlOpener = capturing,
            link = "   ",
            onClick = onClick,
            openConfirmation = false,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onClick() }
        assertNoNodeWithText(dialogTitle)
        assertTrue(capturing.openedUrls.isEmpty())
    }

    @Test
    fun `when dialog confirm clicked then opens uri and invokes onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            externalUrlOpener = noopExternalOpener(),
            link = "https://example.com/confirm",
            onClick = onClick,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onClick() }
        coVerify(exactly = 1) { mainPresenter.navigateToUrlWithLauncher("https://example.com/confirm") }
        assertNoNodeWithText(dialogTitle)
    }

    @Test
    fun `when dialog dismiss clicked then closes dialog without invoking onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            externalUrlOpener = noopExternalOpener(),
            onClick = onClick,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 0) { onClick() }
        assertNoNodeWithText(dialogTitle)
    }

    @Test
    fun `when dialog close button clicked then closes dialog without invoking onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            externalUrlOpener = noopExternalOpener(),
            onClick = onClick,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("close").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 0) { onClick() }
        assertNoNodeWithText(dialogTitle)
    }

    @Test
    fun `when uri open fails then shows snackbar via main presenter and closes dialog without invoking onClick`() {
        mockNavigateToUrlBehavior(mainPresenter, openUrlResult = false)
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            externalUrlOpener = noopExternalOpener(),
            link = "https://example.com/fail",
            onClick = onClick,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { mainPresenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR) }
        verify(exactly = 0) { onClick() }
        assertNoNodeWithText(dialogTitle)
    }

    private fun assertNoNodeWithText(text: String) {
        val nodes =
            composeTestRule
                .onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertTrue(nodes.isEmpty(), "Expected no composable with text \"$text\"")
    }

    private fun noopExternalOpener(): ExternalUrlOpener = ExternalUrlOpener { true }

    private class CapturingExternalUrlOpener : ExternalUrlOpener {
        val openedUrls = mutableListOf<String>()

        override suspend fun openUrl(url: String): Boolean {
            openedUrls += url
            return true
        }
    }
}
