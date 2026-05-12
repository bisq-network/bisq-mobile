package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkDialogSettingsServiceFake
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class NoteTextLinkInteractionUiTest {
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

    private fun webLinkTestModules(
        settings: SettingsServiceFacade,
        mainPresenter: MainPresenter,
    ) = module {
        single<SettingsServiceFacade> { settings }
        single<MainPresenter> { mainPresenter }
    }

    @Test
    fun `when uri link clicked without confirmation then opens uri`() {
        val settings = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        coEvery { mainPresenter.navigateToUrlWithLauncher(any()) } returns true
        startKoin {
            modules(
                webLinkTestModules(settings, mainPresenter),
                module {
                    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
                },
                presentationTestModule,
            )
        }

        val opener = CapturingExternalUrlOpener()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides opener) {
                BisqTheme {
                    NoteText(
                        notes = "Read docs",
                        linkText = "Open link",
                        uri = "https://example.com/note-direct",
                        openConfirmation = false,
                    )
                }
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

        val presenterSpy =
            spyk(WebLinkConfirmationDialogPresenter(settings, mainPresenter))

        startKoin {
            modules(
                webLinkTestModules(settings, mainPresenter),
                module {
                    single<WebLinkConfirmationDialogPresenter> { presenterSpy }
                },
                presentationTestModule,
            )
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                BisqTheme {
                    NoteText(
                        notes = "Read docs",
                        linkText = "Open link",
                        uri = "https://example.com/note-confirm",
                        openConfirmation = true,
                    )
                }
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
