package network.bisq.mobile.presentation.common.ui.components.atoms.button

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LinkButtonUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val dialogTitle get() = "hyperlinks.openInBrowser.attention.headline".i18n()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        initKoin(showWebLinkConfirmation = true)
    }

    private fun initKoin(showWebLinkConfirmation: Boolean) {
        runCatching { stopKoin() }
        val settingsFacade =
            WebLinkDialogSettingsServiceFake(initialShowWebLinkConfirmation = showWebLinkConfirmation)
        startKoin {
            modules(
                module {
                    single<MainPresenter> { mockk(relaxed = true) }
                    single<SettingsServiceFacade> { settingsFacade }
                    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
                },
                presentationTestModule,
            )
        }
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    private fun setLinkButton(
        uriHandler: UriHandler,
        text: String = "Open docs",
        link: String = "https://example.com",
        onClick: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        openConfirmation: Boolean = true,
        forceConfirm: Boolean = false,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                BisqTheme {
                    LinkButton(
                        text = text,
                        link = link,
                        onClick = onClick,
                        onError = onError,
                        openConfirmation = openConfirmation,
                        forceConfirm = forceConfirm,
                    )
                }
            }
        }
    }

    @Test
    fun `when clicked with openConfirmation true then shows confirmation dialog`() {
        setLinkButton(uriHandler = NoopUriHandler())

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("action.dontShowAgain".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when forceConfirm true and showWebLinkConfirmation false then shows confirmation dialog`() {
        initKoin(false)
        setLinkButton(uriHandler = NoopUriHandler(), forceConfirm = true)

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        assertNoNodeWithText("action.dontShowAgain".i18n())
    }

    @Test
    fun `when clicked with openConfirmation false then invokes onClick without dialog`() {
        val capturingUriHandler = CapturingUriHandler()
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            uriHandler = capturingUriHandler,
            onClick = onClick,
            openConfirmation = false,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onClick() }
        assertNoNodeWithText(dialogTitle)
        assertEquals(listOf("https://example.com"), capturingUriHandler.openedUris)
    }

    @Test
    fun `when openConfirmation false and uri open fails then invokes onError with throwable and does not invoke onClick`() {
        val receivedErrors = mutableListOf<Throwable>()
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            uriHandler = ThrowingUriHandler(),
            link = "https://example.com/bypass-fail",
            onClick = onClick,
            onError = { receivedErrors += it },
            openConfirmation = false,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, receivedErrors.size)
        assertEquals("forced openUri failure", receivedErrors.first().message)
        verify(exactly = 0) { onClick() }
        assertNoNodeWithText(dialogTitle)
    }

    @Test
    fun `when openConfirmation false and uri open fails without onError then does not invoke onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            uriHandler = ThrowingUriHandler(),
            link = "https://example.com/bypass-fail",
            onClick = onClick,
            openConfirmation = false,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 0) { onClick() }
        assertNoNodeWithText(dialogTitle)
    }

    @Test
    fun `when dialog confirm clicked then opens uri and invokes onClick`() {
        val capturingUriHandler = CapturingUriHandler()
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            uriHandler = capturingUriHandler,
            link = "https://example.com/confirm",
            onClick = onClick,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onClick() }
        assertEquals(listOf("https://example.com/confirm"), capturingUriHandler.openedUris)
        assertNoNodeWithText(dialogTitle)
    }

    @Test
    fun `when dialog dismiss clicked then closes dialog without invoking onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        setLinkButton(
            uriHandler = NoopUriHandler(),
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
            uriHandler = NoopUriHandler(),
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
    fun `when uri open fails then invokes onError and closes dialog without invoking onClick`() {
        val onClick = mockk<() -> Unit>(relaxed = true)
        val onError = mockk<(Throwable) -> Unit>(relaxed = true)
        setLinkButton(
            uriHandler = ThrowingUriHandler(),
            link = "https://example.com/fail",
            onClick = onClick,
            onError = onError,
        )

        composeTestRule.onNodeWithText("Open docs").performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { onError(any()) }
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

    private class NoopUriHandler : UriHandler {
        override fun openUri(uri: String) {}
    }

    private class CapturingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }

    private class ThrowingUriHandler : UriHandler {
        override fun openUri(uri: String): Unit = throw RuntimeException("forced openUri failure")
    }
}
