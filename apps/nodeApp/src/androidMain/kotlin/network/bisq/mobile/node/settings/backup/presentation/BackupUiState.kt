package network.bisq.mobile.node.settings.backup.presentation

import android.net.Uri

data class BackupUiState(
    val showBackupDialog: Boolean,
    val showWorkingDialog: Boolean,
    val showRestorePasswordDialogForUri: Uri?,
    val errorMessage: String?,
    val passwordForm: BackupPasswordFormState = BackupPasswordFormState(),
    val showNoPasswordConfirmation: Boolean = false,
)

data class BackupPasswordFormState(
    val password: String = "",
    val confirmation: String = "",
    val validationError: String? = null,
    val canSubmit: Boolean = true,
) {
    val isEncrypting: Boolean get() = password.isNotBlank()

    // Keeps the password out of anything that prints the state, such as a log line.
    override fun toString(): String = "BackupPasswordFormState(validationError=$validationError, canSubmit=$canSubmit)"
}
