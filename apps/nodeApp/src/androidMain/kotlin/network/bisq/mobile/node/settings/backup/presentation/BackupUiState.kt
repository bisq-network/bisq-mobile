package network.bisq.mobile.node.settings.backup.presentation

import android.net.Uri

data class BackupUiState(
    val showBackupDialog: Boolean,
    val showWorkingDialog: Boolean,
    val showRestorePasswordDialogForUri: Uri?,
    val errorMessage: String?,
)
