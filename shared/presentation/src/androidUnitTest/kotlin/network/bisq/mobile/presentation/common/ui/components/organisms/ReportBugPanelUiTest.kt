package network.bisq.mobile.presentation.common.ui.components.organisms

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [ReportBugPanelContent], covering the log actions added next to the error message:
 * copy to clipboard, save to file, and the inline status text (snackbars are hidden behind the dialog).
 */
class ReportBugPanelUiTest : BisqComposeUiTestBase() {
    private val errorMessage = "java.lang.IllegalStateException: boom"
    private val logFileHint get() = "mobile.genericError.reportInfo.logFile".i18n()

    @Test
    fun `when copy icon clicked then it confirms with a check icon`() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Copy icon").performClick()

        composeTestRule.onNodeWithContentDescription("check").assertExists()
    }

    @Test
    fun `when share icon clicked then onSaveToFile invoked`() {
        val onSaveToFile = mockk<() -> Unit>(relaxed = true)

        setContent(onSaveToFile = onSaveToFile)

        composeTestRule.onNodeWithContentDescription("share").performClick()

        verify(exactly = 1) { onSaveToFile() }
    }

    @Test
    fun `when a log file exists then it is offered above the error message`() {
        val onShareLogFile = mockk<() -> Unit>(relaxed = true)
        val onSaveToFile = mockk<() -> Unit>(relaxed = true)

        setContent(logFileName = "bisq.log", onSaveToFile = onSaveToFile, onShareLogFile = onShareLogFile)

        composeTestRule.onAllNodesWithContentDescription("share")[0].performClick()

        verify(exactly = 1) { onShareLogFile() }
        verify(exactly = 0) { onSaveToFile() }
    }

    @Test
    fun `without a log file only the error message can be shared`() {
        setContent()

        composeTestRule.onAllNodesWithContentDescription("share").assertCountEquals(1)
    }

    @Test
    fun `without a log file the log file hint is hidden`() {
        setContent()

        composeTestRule.onNodeWithText(logFileHint).assertDoesNotExist()
    }

    @Test
    fun `with a log file the log file hint is shown`() {
        setContent(logFileName = "bisq.log")

        composeTestRule.onNodeWithText(logFileHint).assertExists()
    }

    @Test
    fun `when status message given then it is shown`() {
        setContent(statusMessage = "Could not save the error log")

        composeTestRule.onNodeWithText("Could not save the error log").assertExists()
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        statusMessage: String? = null,
        logFileName: String? = null,
        onSaveToFile: () -> Unit = {},
        onShareLogFile: () -> Unit = {},
    ) {
        setTestContent {
            ReportBugPanelContent(
                errorMessage = errorMessage,
                isUncaughtException = false,
                isIOS = false,
                statusMessage = statusMessage,
                logFileName = logFileName,
                onClose = {},
                onShutdown = {},
                onSaveToFile = onSaveToFile,
                onShareLogFile = onShareLogFile,
                onReport = {},
            )
        }
    }
}
