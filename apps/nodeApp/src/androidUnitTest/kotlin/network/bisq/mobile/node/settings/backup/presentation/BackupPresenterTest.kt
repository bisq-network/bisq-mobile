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
        presenter = BackupPresenter(mainPresenter, nodeBackupServiceFacade)
        presenter.onAction(BackupUiAction.ShowBackupDialog)
    }

    private fun enterPassword(
        password: String,
        confirmation: String = password,
    ) {
        presenter.onAction(BackupUiAction.OnBackupPasswordChange(password))
        presenter.onAction(BackupUiAction.OnBackupPasswordConfirmationChange(confirmation))
    }

    @Test
    fun `when the form is empty then it can be submitted without encryption`() {
        val form = presenter.uiState.value.passwordForm

        assertNull(form.validationError)
        assertTrue(form.canSubmit)
        assertFalse(form.isEncrypting)
    }

    @Test
    fun `when the password is too short then it shows an error and cannot be submitted`() {
        enterPassword("1234567")

        val form = presenter.uiState.value.passwordForm
        assertEquals("validation.password.tooShort".i18n(), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when the confirmation does not match then it shows an error and cannot be submitted`() {
        enterPassword("long enough password", "long enough passwort")

        val form = presenter.uiState.value.passwordForm
        assertEquals("validation.password.notMatching".i18n(), form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when the confirmation is still empty then it cannot be submitted without an error`() {
        presenter.onAction(BackupUiAction.OnBackupPasswordChange("long enough password"))

        val form = presenter.uiState.value.passwordForm
        assertNull(form.validationError)
        assertFalse(form.canSubmit)
    }

    @Test
    fun `when a valid password is submitted then the backup is encrypted with it`() =
        runTest {
            enterPassword("long enough password")

            presenter.onAction(BackupUiAction.OnBackupClick)
            advanceUntilIdle()

            coVerify { nodeBackupServiceFacade.backupDataDir("long enough password") }
            val state = presenter.uiState.value
            assertFalse(state.showBackupDialog)
            assertEquals(BackupPasswordFormState(), state.passwordForm)
        }

    @Test
    fun `when an invalid form is submitted then no backup is made`() =
        runTest {
            enterPassword("1234567")

            presenter.onAction(BackupUiAction.OnBackupClick)
            advanceUntilIdle()

            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }

    @Test
    fun `when submitted without a password then it asks for confirmation before backing up`() =
        runTest {
            presenter.onAction(BackupUiAction.OnBackupClick)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.showNoPasswordConfirmation)
            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }

    @Test
    fun `when the unencrypted backup is confirmed then it backs up without a password`() =
        runTest {
            presenter.onAction(BackupUiAction.OnBackupClick)

            presenter.onAction(BackupUiAction.ConfirmBackupWithoutPassword)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showNoPasswordConfirmation)
            coVerify { nodeBackupServiceFacade.backupDataDir(null) }
        }

    @Test
    fun `when the unencrypted backup is dismissed then no backup is made`() =
        runTest {
            presenter.onAction(BackupUiAction.OnBackupClick)

            presenter.onAction(BackupUiAction.DismissBackupWithoutPasswordConfirmation)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showNoPasswordConfirmation)
            assertTrue(presenter.uiState.value.showBackupDialog)
            verify(exactly = 0) { nodeBackupServiceFacade.backupDataDir(any()) }
        }

    @Test
    fun `when the dialog is reopened then the previous password is gone`() {
        enterPassword("long enough password")

        presenter.onAction(BackupUiAction.DismissBackupDialog)
        presenter.onAction(BackupUiAction.ShowBackupDialog)

        assertEquals(BackupPasswordFormState(), presenter.uiState.value.passwordForm)
    }

    @Test
    fun `when the form state is printed then the password is not in it`() {
        enterPassword("long enough password")

        assertFalse("long enough password" in presenter.uiState.value.toString())
    }
}
