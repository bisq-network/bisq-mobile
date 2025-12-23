package network.bisq.mobile.node.settings.backup.presentation

import android.net.Uri

sealed interface BackupUiActions {
    data object ShowBackupDialog : BackupUiActions
    data object DismissBackupDialog : BackupUiActions
    data class ShowError(val message: String) : BackupUiActions
    data object ClearError : BackupUiActions
    data class OnBackupToFile(val password: String?) : BackupUiActions
    data class OnRestoreFromFileActivityResult(val uri: Uri?) : BackupUiActions
    data class ShowRestorePasswordDialog(val uri: Uri) : BackupUiActions
    data object DismissRestorePasswordDialog : BackupUiActions
    data class OnStartRestore(val uri: Uri, val password: String?) : BackupUiActions
}