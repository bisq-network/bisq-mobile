package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI tests for [WebLinkConfirmationDialog] lifecycle using the [ReputationScreen] pattern:
 * a nullable link state gates the dialog, and each callback (onConfirm, onDismiss, onError)
 * resets it to null to dismiss the dialog.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WebLinkConfirmationDialogLifecycleUiTest : PresentationKoinComposeTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var settingsFacade: WebLinkDialogSettingsServiceFake

    override fun additionalModules(): List<Module> = listOf(webLinkConfirmationTestModule({ mainPresenter }, { settingsFacade }))

    override fun onKoinReady() {
        settingsFacade = WebLinkDialogSettingsServiceFake()
        mainPresenter = mockk(relaxed = true)
        mockNavigateToUrlBehavior(mainPresenter, openUrlResult = true)
    }

    private val dialogTitle get() = "hyperlinks.openInBrowser.attention.headline".i18n()

    private fun setDialogContent(
        selectedWebLink: () -> String?,
        onClear: () -> Unit,
        onError: () -> Unit = onClear,
    ) {
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
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

    private fun assertNoDialog() {
        val nodes =
            composeTestRule
                .onAllNodesWithText(dialogTitle)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
        assertTrue(nodes.isEmpty(), "Expected dialog to be dismissed")
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
        coVerify(exactly = 0) { mainPresenter.navigateToUrlWithLauncher(any()) }
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
        coVerify(exactly = 1) { mainPresenter.navigateToUrlWithLauncher("https://example.com/confirm") }
    }

    @Test
    fun `error callback clears selected link and closes dialog when uri open fails`() {
        mockNavigateToUrlBehavior(mainPresenter, openUrlResult = false)
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
        coVerify(exactly = 1) { mainPresenter.navigateToUrlWithLauncher("https://example.com/error") }
    }
}
