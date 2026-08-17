package network.bisq.mobile.presentation.common.ui.components.organisms

import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.share.AppLogFile
import network.bisq.mobile.presentation.common.share.AppLogFileProvider
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Covers [ReportBugPanel]'s wiring: which service each action reaches, and that a log file is only
 * offered when the platform provides one. [ReportBugPanelUiTest] covers the stateless content.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportBugPanelKoinUiTest : PresentationKoinComposeTestBase() {
    private lateinit var appPresenter: AppPresenter
    private lateinit var shareFileService: ShareFileService
    private lateinit var logFileProvider: AppLogFileProvider

    private val errorMessage = "java.lang.IllegalStateException: boom"
    private val logFile = AppLogFile(path = "/data/data/app/files/bisq.log", name = "bisq.log")

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<AppPresenter> { appPresenter }
                single<ShareFileService> { shareFileService }
                single<AppLogFileProvider> { logFileProvider }
            },
        )

    override fun onKoinReady() {
        super.onKoinReady()
        appPresenter = mockk(relaxed = true)
        shareFileService = mockk(relaxed = true)
        logFileProvider = mockk(relaxed = true)
        every { appPresenter.isIOS() } returns false
        coEvery { logFileProvider.logFile() } returns null
        coEvery { shareFileService.shareUtf8TextFile(any(), any(), any()) } returns Result.success(Unit)
        coEvery { shareFileService.shareFile(any()) } returns Result.success(Unit)
    }

    @Test
    fun `when the error is shared then it is exported as a text file`() {
        setPanel()

        composeTestRule.onNodeWithContentDescription("share").performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { shareFileService.shareUtf8TextFile(errorMessage, any(), errorMessage) }
    }

    @Test
    fun `when the share fails then the failure is shown inline`() {
        coEvery { shareFileService.shareUtf8TextFile(any(), any(), any()) } returns Result.failure(IllegalStateException("nope"))
        setPanel()

        composeTestRule.onNodeWithContentDescription("share").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.genericError.saveToFile.failed".i18n()).assertExists()
    }

    @Test
    fun `without a log file none is offered`() {
        setPanel()

        composeTestRule.onNodeWithText("mobile.genericError.logFile".i18n(logFile.name)).assertDoesNotExist()
    }

    @Test
    fun `when a log file exists then sharing it hands its path to the share service`() {
        coEvery { logFileProvider.logFile() } returns logFile
        setPanel()

        composeTestRule.onNodeWithText("mobile.genericError.logFile".i18n(logFile.name)).assertExists()
        composeTestRule.onAllNodesWithContentDescription("share")[0].performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { shareFileService.shareFile(logFile.path) }
        coVerify(exactly = 0) { shareFileService.shareUtf8TextFile(any(), any(), any()) }
    }

    @Test
    fun `when reporting then the issue tracker is opened`() {
        setPanel()

        composeTestRule.onNodeWithText("support.reports.title".i18n()).performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { appPresenter.navigateToReportError() }
    }

    @Test
    fun `when the crash was uncaught then the app is shut down instead of dismissed`() {
        setPanel(isUncaughtException = true)

        composeTestRule.onNodeWithText("action.shutDown".i18n()).performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) { appPresenter.onTerminateApp() }
    }

    private fun setPanel(isUncaughtException: Boolean = false) {
        setTestContent {
            ReportBugPanel(
                errorMessage = errorMessage,
                isUncaughtException = isUncaughtException,
                onClose = {},
            )
        }
    }
}
