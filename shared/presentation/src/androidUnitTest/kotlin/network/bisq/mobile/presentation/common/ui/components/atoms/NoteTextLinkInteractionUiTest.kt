package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.presentation.common.test_utils.compose.PresentationKoinComposeTestBase
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkDialogSettingsServiceFake
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.webLinkConfirmationTestModule
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import org.koin.dsl.module
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NoteTextLinkInteractionUiTest : PresentationKoinComposeTestBase() {
    @Test
    fun `when uri link clicked without confirmation then opens uri`() {
        val settings = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns true
        restartKoinWith(webLinkConfirmationTestModule({ mainPresenter }, { settings }))

        val opener = CapturingExternalUrlOpener()
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides opener) {
                NoteText(
                    notes = "Read docs",
                    linkText = "Open link",
                    uri = "https://example.com/note-direct",
                    openConfirmation = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Open link", substring = true).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("https://example.com/note-direct"), opener.openedUrls)
    }

    @Test
    fun `when uri link clicked with confirmation then presenter navigates to url`() {
        val settings = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns true
        val presenterSpy = spyk(WebLinkConfirmationDialogPresenter(settings, mainPresenter))

        restartKoinWith(
            module {
                single { settings }
                single { mainPresenter }
                single<WebLinkConfirmationDialogPresenter> { presenterSpy }
            },
        )

        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                NoteText(
                    notes = "Read docs",
                    linkText = "Open link",
                    uri = "https://example.com/note-confirm",
                    openConfirmation = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Open link", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) {
            presenterSpy.navigateToUrlAwait("https://example.com/note-confirm")
        }
    }

    private class CapturingExternalUrlOpener : ExternalUrlOpener {
        val openedUrls = mutableListOf<String>()

        override suspend fun openUrl(url: String): Boolean {
            openedUrls += url
            return true
        }
    }
}
