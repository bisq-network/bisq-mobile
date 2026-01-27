package network.bisq.mobile.node.settings.backup.presentation

import android.net.Uri

sealed interface BackupUiAction {
    data object ShowBackupDialog : BackupUiAction

    data object DismissBackupDialog : BackupUiAction

    data class ShowError(
        val message: String,
    ) : BackupUiAction

    data object ClearError : BackupUiAction

    data class OnBackupToFile(
        val password: String?,
    ) : BackupUiAction

    data class OnRestoreFromFileActivityResult(
        val uri: Uri?,
    ) : BackupUiAction

    data class ShowRestorePasswordDialog(
        val uri: Uri,
    ) : BackupUiAction

    data object DismissRestorePasswordDialog : BackupUiAction

    data class OnStartRestore(
        val uri: Uri,
        val password: String?,
    ) : BackupUiAction
}
