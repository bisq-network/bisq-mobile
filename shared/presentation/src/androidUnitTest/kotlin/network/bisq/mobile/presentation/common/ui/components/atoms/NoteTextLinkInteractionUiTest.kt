package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.di.presentationTestModule
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
        every { mainPresenter.navigateToUrl(any()) } returns true
        startKoin {
            modules(
                webLinkTestModules(settings, mainPresenter),
                module {
                    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
                },
                presentationTestModule,
            )
        }

        val handler = CapturingUriHandler()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
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

        assertEquals(listOf("https://example.com/note-direct"), handler.openedUris)
    }

    @Test
    fun `when uri link clicked with confirmation then presenter navigates to url`() {
        val settings = WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = true)
        val mainPresenter = mockk<MainPresenter>(relaxed = true)
        every { mainPresenter.navigateToUrl(any()) } returns true

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
            CompositionLocalProvider(LocalUriHandler provides NoopUriHandler()) {
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

        verify(exactly = 1) {
            presenterSpy.navigateToUrl("https://example.com/note-confirm")
        }
    }

    private class CapturingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }

    private class NoopUriHandler : UriHandler {
        override fun openUri(uri: String) {}
    }
}
