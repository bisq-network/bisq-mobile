package network.bisq.mobile.node.settings.backup.presentation

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import network.bisq.mobile.node.settings.backup.domain.NodeBackupServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackupPresenterTest : NodeKoinIntegrationTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val nodeBackupServiceFacade: NodeBackupServiceFacade = mockk(relaxed = true)

    private lateinit var presenter: BackupPresenter

    override fun onSetup() {
        every { nodeBackupServiceFacade.backupDataDir(any()) } returns CompletableDeferred<Throwable?>(value = null)
    }

    private fun openDialog(isPasswordRequired: Boolean) {
        presenter = BackupPresenter(mainPresenter, nodeBackupServiceFacade, isPasswordRequired)
        presenter.onAction(BackupUiAction.ShowBackupDialog)
    }

    private fun enterPassword(
        password: String,
        confirmation: String = password,
    ) {
        presenter.onAction(BackupUiAction.OnBackupPasswordChange(password))
        presenter.onAction(BackupUiAction.OnBackupPasswordConfirmationChange(confirmation))
    }

    // Both build types

    @Test
    fun `when the password is too short then it shows an error and cannot be submitted`() {
        openDialog(isPasswordRequired = true)

        enterPassword("abcdefghijk")

        val form = presenter.uiState.value.passwordForm
        assertEquals("mobile.resources.backup.password.tooShort".i18n(12), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when the password is too long then it shows an error and cannot be submitted`() {
        openDialog(isPasswordRequired = true)

        enterPassword("abcde".repeat(52))

        val form = presenter.uiState.value.passwordForm
        assertEquals("mobile.resources.backup.password.tooLong".i18n(256), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when the password has too few distinct characters then it shows an error and cannot be submitted`() {
        openDialog(isPasswordRequired = true)

        enterPassword("aaaaaaaaaaaa")

        val form = presenter.uiState.value.passwordForm
        assertEquals("mobile.resources.backup.password.tooFewDistinctCharacters".i18n(5), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when the confirmation does not match then it shows an error and cannot be submitted`() {
        openDialog(isPasswordRequired = true)

        enterPassword("long enough password", "long enough passwort")

        val form = presenter.uiState.value.passwordForm
        assertEquals("validation.password.notMatching".i18n(), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when surrounding spaces pad a short password then it is still too short`() {
        openDialog(isPasswordRequired = true)

        enterPassword("  abcdefghijk  ")

        val form = presenter.uiState.value.passwordForm
        assertEquals("mobile.resources.backup.password.tooShort".i18n(12), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when the fields differ only by surrounding spaces then they match`() {
        openDialog(isPasswordRequired = true)

        enterPassword("long enough password ", "long enough password")

        val form = presenter.uiState.value.passwordForm
        assertNull(form.validationError)
        assertTrue(form.canSubmit)
    }

    @Test
    fun `when the confirmation is still empty then it cannot be submitted without an error`() {
        openDialog(isPasswordRequired = true)

        presenter.onAction(BackupUiAction.OnBackupPasswordChange("long enough password"))

        val form = presenter.uiState.value.passwordForm
        assertNull(form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when a valid password is submitted then the backup is encrypted with it`() =
        runTest {
            openDialog(isPasswordRequired = true)
            enterPassword("long enough password")

            presenter.onAction(BackupUiAction.OnBackupClick)
            advanceUntilIdle()

            coVerify { nodeBackupServiceFacade.backupDataDir("long enough password") }
            val state = presenter.uiState.value
            assertFalse(state.showBackupDialog)
            assertEquals("", state.passwordForm.password)
        }

    @Test
    fun `when an invalid form is submitted then no backup is made`() =
        runTest {
            openDialog(isPasswordRequired = false)
            enterPassword("abcdefghijk")

            presenter.onAction(BackupUiAction.OnBackupClick)
            advanceUntilIdle()

            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }

    @Test
    fun `when the dialog is reopened then the previous password is gone`() {
        openDialog(isPasswordRequired = true)
        enterPassword("long enough password")

        presenter.onAction(BackupUiAction.DismissBackupDialog)
        presenter.onAction(BackupUiAction.ShowBackupDialog)

        assertEquals(BackupPasswordFormState(canSubmit = false), presenter.uiState.value.passwordForm)
    }

    @Test
    fun `when the form state is printed then the password is not in it`() {
        openDialog(isPasswordRequired = true)

        enterPassword("long enough password")

        assertFalse("long enough password" in presenter.uiState.value.toString())
    }

    // Release builds

    @Test
    fun `when a password is required then the empty form cannot be submitted`() {
        openDialog(isPasswordRequired = true)

        val state = presenter.uiState.value
        assertTrue(state.isPasswordRequired)
        assertNull(state.passwordForm.validationError)
        assertFalse(state.passwordForm.canSubmit)
    }

    @Test
    fun `when a password is required then submitting without one neither backs up nor offers to skip`() =
        runTest {
            openDialog(isPasswordRequired = true)

            presenter.onAction(BackupUiAction.OnBackupClick)
            presenter.onAction(BackupUiAction.ConfirmBackupWithoutPassword)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showNoPasswordConfirmation)
            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }

    @Test
    fun `when a password is required then clearing the fields makes the form unsubmittable again`() {
        openDialog(isPasswordRequired = true)
        enterPassword("long enough password")

        enterPassword("")

        assertFalse(presenter.uiState.value.passwordForm.canSubmit)
    }

    // Debug builds

    @Test
    fun `when no password is required then the empty form can be submitted without encryption`() {
        openDialog(isPasswordRequired = false)

        val state = presenter.uiState.value
        assertFalse(state.isPasswordRequired)
        assertNull(state.passwordForm.validationError)
        assertTrue(state.passwordForm.canSubmit)
        assertFalse(state.passwordForm.isEncrypting)
    }

    @Test
    fun `when no password is required then submitting without one asks for confirmation first`() =
        runTest {
            openDialog(isPasswordRequired = false)

            presenter.onAction(BackupUiAction.OnBackupClick)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.showNoPasswordConfirmation)
            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }

    @Test
    fun `when the unencrypted backup is confirmed then it backs up without a password`() =
        runTest {
            openDialog(isPasswordRequired = false)
            presenter.onAction(BackupUiAction.OnBackupClick)

            presenter.onAction(BackupUiAction.ConfirmBackupWithoutPassword)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showNoPasswordConfirmation)
            coVerify { nodeBackupServiceFacade.backupDataDir(null) }
        }

    @Test
    fun `when the unencrypted backup is dismissed then no backup is made`() =
        runTest {
            openDialog(isPasswordRequired = false)
            presenter.onAction(BackupUiAction.OnBackupClick)

            presenter.onAction(BackupUiAction.DismissBackupWithoutPasswordConfirmation)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showNoPasswordConfirmation)
            assertTrue(presenter.uiState.value.showBackupDialog)
            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }
}
