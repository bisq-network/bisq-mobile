package network.bisq.mobile.node.settings.backup.presentation

import android.view.Window
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import network.bisq.mobile.test.presentation.compose.CaptureHostWindow
import network.bisq.mobile.test.presentation.compose.isSecure
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * The screen resolves its presenter through `koinInject`, so the mock is loaded into the Koin
 * context `TestApplication` owns rather than through a base that would start a second one.
 */
@Config(application = TestApplication::class)
class BackupScreenUiTest : BisqComposeUiTestBase() {
    private fun loadPresenter(state: BackupUiState) {
        val presenter =
            mockk<BackupPresenter>(relaxed = true) {
                every { uiState } returns MutableStateFlow(state)
            }
        loadKoinModules(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                // TopBar reads both, and the node test module's relaxed mock cannot type them.
                single<NavigationManager> {
                    mockk(relaxed = true) {
                        every { currentTab } returns MutableStateFlow(null)
                        every { showBackButton() } returns false
                    }
                }
                single { presenter }
            },
        )
    }

    private fun backupDialogState(
        isPasswordRequired: Boolean,
        passwordForm: BackupPasswordFormState = BackupPasswordFormState(canSubmit = !isPasswordRequired),
        showNoPasswordConfirmation: Boolean = false,
    ) = BackupUiState(
        showBackupDialog = true,
        showWorkingDialog = false,
        showRestorePasswordDialogForUri = null,
        errorMessage = null,
        passwordForm = passwordForm,
        showNoPasswordConfirmation = showNoPasswordConfirmation,
        isPasswordRequired = isPasswordRequired,
    )

    @Test
    fun `backup screen blocks screenshots`() {
        loadPresenter(
            BackupUiState(
                showBackupDialog = false,
                showWorkingDialog = false,
                showRestorePasswordDialogForUri = null,
                errorMessage = null,
            ),
        )
        lateinit var window: Window

        setTestContent {
            CaptureHostWindow { window = it }
            BackupScreen()
        }

        assertTrue(window.isSecure)
    }

    @Test
    fun `when a password is required then the empty dialog explains it and cannot be submitted`() {
        loadPresenter(backupDialogState(isPasswordRequired = true))

        setTestContent { BackupScreen() }

        composeTestRule.onNodeWithText("mobile.resources.backup.password.info.required".i18n(12)).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.resources.backup.password.button.encryptAndBackup".i18n()).assertIsNotEnabled()
    }

    @Test
    fun `when no password is required then the empty dialog can back up without encryption`() {
        loadPresenter(backupDialogState(isPasswordRequired = false))

        setTestContent { BackupScreen() }

        composeTestRule.onNodeWithText("mobile.resources.backup.password.info".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.resources.backup.password.button.backup".i18n()).assertIsEnabled()
    }

    @Test
    fun `when the password is invalid then the dialog shows why`() {
        val error = "mobile.resources.backup.password.tooShort".i18n(12)
        loadPresenter(
            backupDialogState(
                isPasswordRequired = true,
                passwordForm = BackupPasswordFormState(password = "short", validationError = error),
            ),
        )

        setTestContent { BackupScreen() }

        composeTestRule.onAllNodesWithText(error).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.resources.backup.password.button.encryptAndBackup".i18n()).assertIsNotEnabled()
    }

    @Test
    fun `when an unencrypted backup is pending then the warning is shown`() {
        loadPresenter(backupDialogState(isPasswordRequired = false, showNoPasswordConfirmation = true))

        setTestContent { BackupScreen() }

        composeTestRule.onNodeWithText("mobile.resources.backup.noPassword.confirmation".i18n()).assertIsDisplayed()
    }
}
