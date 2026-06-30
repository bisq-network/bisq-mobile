package network.bisq.mobile.presentation.startup.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashDialogsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockOnAction: (SplashUiAction) -> Unit

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        mockOnAction = mockk(relaxed = true)
    }

    // region TimeoutIos

    @Test
    fun `when timeout ios dialog then shows title and ios message`() {
        setDialog(SplashActiveDialog.TimeoutIos)

        composeTestRule.onNodeWithText("mobile.bootstrap.timeout.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.bootstrap.timeout.message.ios".i18n(SAMPLE_STAGE)).assertIsDisplayed()
    }

    @Test
    fun `when timeout ios dialog then has no dismiss button`() {
        setDialog(SplashActiveDialog.TimeoutIos)

        composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).assertDoesNotExist()
    }

    @Test
    fun `when timeout ios confirm clicked then continues waiting`() {
        setDialog(SplashActiveDialog.TimeoutIos)

        composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnTimeoutDialogContinue) }
    }

    // endregion

    // region TimeoutAndroid

    @Test
    fun `when timeout android dialog then shows title and message`() {
        setDialog(SplashActiveDialog.TimeoutAndroid)

        composeTestRule.onNodeWithText("mobile.bootstrap.timeout.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.bootstrap.timeout.message".i18n(SAMPLE_STAGE)).assertIsDisplayed()
    }

    @Test
    fun `when timeout android confirm clicked then restarts app`() {
        setDialog(SplashActiveDialog.TimeoutAndroid)

        composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnRestartApp) }
    }

    @Test
    fun `when timeout android dismiss clicked then continues waiting`() {
        setDialog(SplashActiveDialog.TimeoutAndroid)

        composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnTimeoutDialogContinue) }
    }

    // endregion

    // region TorBootstrapFailed

    @Test
    fun `when tor failed dialog then shows title and message`() {
        setDialog(SplashActiveDialog.TorBootstrapFailed)

        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.message".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when tor failed confirm clicked then purges and restarts tor`() {
        setDialog(SplashActiveDialog.TorBootstrapFailed)

        composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnPurgeRestartTor) }
    }

    @Test
    fun `when tor failed dismiss clicked then restarts tor`() {
        setDialog(SplashActiveDialog.TorBootstrapFailed)

        composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnRestartTor) }
    }

    // endregion

    // region BootstrapFailedIos

    @Test
    fun `when bootstrap failed ios dialog then shows title and message`() {
        setDialog(SplashActiveDialog.BootstrapFailedIos)

        composeTestRule.onNodeWithText("mobile.bootstrap.failed.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.bootstrap.failed.message".i18n(SAMPLE_STAGE)).assertIsDisplayed()
    }

    @Test
    fun `when bootstrap failed ios dialog then has no buttons`() {
        setDialog(SplashActiveDialog.BootstrapFailedIos)

        composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).assertDoesNotExist()
    }

    // endregion

    // region BootstrapFailedAndroid

    @Test
    fun `when bootstrap failed android dialog then shows title and message`() {
        setDialog(SplashActiveDialog.BootstrapFailedAndroid)

        composeTestRule.onNodeWithText("mobile.bootstrap.failed.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.bootstrap.failed.message".i18n(SAMPLE_STAGE)).assertIsDisplayed()
    }

    @Test
    fun `when bootstrap failed android confirm clicked then restarts app`() {
        setDialog(SplashActiveDialog.BootstrapFailedAndroid)

        composeTestRule.onNodeWithContentDescription(DIALOG_CONFIRM).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnRestartApp) }
    }

    @Test
    fun `when bootstrap failed android dismiss clicked then terminates app`() {
        setDialog(SplashActiveDialog.BootstrapFailedAndroid)

        composeTestRule.onNodeWithContentDescription(DIALOG_DISMISS).performClick()

        verify(exactly = 1) { mockOnAction(SplashUiAction.OnTerminateApp) }
    }

    // endregion

    @Test
    fun `when no active dialog then renders nothing`() {
        setDialog(null)

        composeTestRule.onNodeWithText("mobile.bootstrap.timeout.title".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText("mobile.bootstrap.tor.failed.title".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText("mobile.bootstrap.failed.title".i18n()).assertDoesNotExist()
    }

    private fun setDialog(dialog: SplashActiveDialog?) {
        setTestContent {
            SplashDialogs(
                uiState = sampleUiState(dialog),
                onAction = mockOnAction,
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    private fun sampleUiState(
        dialog: SplashActiveDialog?,
        stage: String = SAMPLE_STAGE,
    ): SplashUiState =
        SplashUiState(
            activeDialog = dialog,
            currentBootstrapStage = stage,
        )

    private companion object {
        const val SAMPLE_STAGE = "tor"
        const val DIALOG_CONFIRM = "dialog_confirm_yes"
        const val DIALOG_DISMISS = "dialog_confirm_no"
    }
}
