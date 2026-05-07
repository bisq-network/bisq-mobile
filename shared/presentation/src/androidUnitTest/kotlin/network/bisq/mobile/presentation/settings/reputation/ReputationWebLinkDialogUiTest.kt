package network.bisq.mobile.presentation.settings.reputation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialog
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the selectedWebLink-driven dialog lifecycle pattern used by [ReputationScreen]:
 * a nullable link state gates [WebLinkConfirmationDialog], and each callback (onConfirm,
 * onDismiss, onError) resets it to null to dismiss the dialog.
 */
@RunWith(AndroidJUnit4::class)
class ReputationWebLinkDialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var uriHandler: NoopUriHandler
    private lateinit var mainPresenter: MainPresenter

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        uriHandler = NoopUriHandler()
        initKoin(openUrlResult = true)
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun `dismiss callback clears selected link and closes dialog`() {
        var selectedWebLink by mutableStateOf<String?>(null)
        setDialogContent(selectedWebLink = { selectedWebLink }, onClear = { selectedWebLink = null })

        composeTestRule.runOnIdle { selectedWebLink = "https://example.com/dismiss" }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.waitForIdle()
        assertNoDialog()
        verify(exactly = 0) { mainPresenter.navigateToUrl(any()) }
    }

    @Test
    fun `confirm callback opens uri clears selected link and closes dialog`() {
        var selectedWebLink by mutableStateOf<String?>(null)
        setDialogContent(selectedWebLink = { selectedWebLink }, onClear = { selectedWebLink = null })

        composeTestRule.runOnIdle { selectedWebLink = "https://example.com/confirm" }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()
        assertNoDialog()
        verify(exactly = 1) { mainPresenter.navigateToUrl("https://example.com/confirm") }
    }

    @Test
    fun `error callback clears selected link and closes dialog when uri open fails`() {
        initKoin(openUrlResult = false)
        var selectedWebLink by mutableStateOf<String?>(null)
        var errorFlag = false
        var clearedFlag = false
        setDialogContent(
            selectedWebLink = { selectedWebLink },
            onClear = {
                selectedWebLink = null
                clearedFlag = true
            },
            onError = {
                selectedWebLink = null
                errorFlag = true
            },
        )

        composeTestRule.runOnIdle { selectedWebLink = "https://example.com/error" }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()
        assertNoDialog()
        assertTrue(errorFlag)
        assertFalse(clearedFlag)
        verify(exactly = 1) { mainPresenter.navigateToUrl("https://example.com/error") }
    }

    private val dialogTitle get() = "hyperlinks.openInBrowser.attention.headline".i18n()

    private fun setDialogContent(
        selectedWebLink: () -> String?,
        onClear: () -> Unit,
        onError: () -> Unit = onClear,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                BisqTheme {
                    selectedWebLink()?.let { webLink ->
                        WebLinkConfirmationDialog(
                            link = webLink,
                            onConfirm = { onClear() },
                            onDismiss = { onClear() },
                            onError = { onError() },
                        )
                    }
                }
            }
        }
    }

    private fun initKoin(openUrlResult: Boolean) {
        runCatching { stopKoin() }
        mainPresenter = mockk(relaxed = true)
        every { mainPresenter.navigateToUrl(any()) } returns openUrlResult
        startKoin {
            modules(
                module {
                    single<MainPresenter> { mainPresenter }
                    single<SettingsServiceFacade> { WebLinkDialogSettingsServiceFake() }
                    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
                },
                presentationTestModule,
            )
        }
    }

    private fun assertNoDialog() {
        val nodes =
            composeTestRule
                .onAllNodesWithText(dialogTitle)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertTrue(nodes.isEmpty(), "Expected dialog to be dismissed")
    }

    private class NoopUriHandler : UriHandler {
        override fun openUri(uri: String) = Unit
    }
}
