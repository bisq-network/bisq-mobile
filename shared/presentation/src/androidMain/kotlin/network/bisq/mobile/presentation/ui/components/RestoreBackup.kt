package network.bisq.mobile.presentation.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

const val backupPrefix = "bisq2_mobile-backup-"

@Composable
actual fun RestoreBackup(onRestoreBackup: (String, String?, ByteArray) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val log: Logger = remember { getLogger("ImportBackupFile") }
    val onRestoreUpdated by rememberUpdatedState(onRestoreBackup)

    var showPasswordOverlay: Boolean by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }

    var selectedFileName: String? by remember { mutableStateOf(null) }
    var selectedFileData: ByteArray? by remember { mutableStateOf(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                try {
                    // Persist access across restarts
                    context.contentResolver.takePersistableUriPermission(
                        selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    log.e(e) { "takePersistableUriPermission failed" }
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        val size = context.contentResolver.openFileDescriptor(selectedUri, "r").use { it?.statSize } ?: 0
                        if (size > 10 * 1024 * 1024) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "mobile.resources.restore.error.fileSizeTooLarge".i18n()
                            }
                            return@launch
                        }

                        val bytes = context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            input.readBytes()
                        }
                        if (bytes == null) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "mobile.resources.restore.error.cannotReadFile".i18n()
                            }
                            return@launch
                        }

                        val fileName = getFileName(context, selectedUri)
                        val isValid = fileName.startsWith(backupPrefix) &&
                                (fileName.endsWith(".enc") || fileName.endsWith(".zip"))

                        if (!isValid) {
                            log.e { "Invalid backup file name: $fileName" }
                            withContext(Dispatchers.Main) {
                                errorMessage = "mobile.resources.restore.error.invalidFileName".i18n()
                            }
                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            if (fileName.endsWith(".enc")) {
                                selectedFileName = fileName
                                selectedFileData = bytes
                                showPasswordOverlay = true
                            } else {
                                onRestoreBackup(fileName, null, bytes)
                            }
                        }
                    } catch (e: Exception) {
                        log.e(e) { "Importing backup failed" }
                        withContext(Dispatchers.Main) {
                            errorMessage = "mobile.resources.restore.error".i18n(e.message ?: e.toString())
                        }
                    }
                }
            }
        }
    )

    BisqButton(
        text = "mobile.resources.restore.button".i18n(),
        // Wildcard MIME type for maximum compatibility
        onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
        type = BisqButtonType.Outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
    )

    errorMessage?.let { message ->
        ErrorOverlay(message) { errorMessage = null }
    }

    if (showPasswordOverlay) {
        PasswordOverlay(
            onPassword = { password ->
                val fileName = selectedFileName
                val data = selectedFileData
                if (fileName != null && data != null) {
                    onRestoreUpdated(fileName, password, data)
                }
                showPasswordOverlay = false
                selectedFileName = null
                selectedFileData = null
            },
            onDismissOverlay = {
                showPasswordOverlay = false
                selectedFileName = null
                selectedFileData = null
            }
        )
    }
}

@Composable
fun PasswordOverlay(
    onPassword: (String?) -> Unit,
    onDismissOverlay: () -> Unit,
) {
    var password: String by remember { mutableStateOf("") }

    BisqDialog(
        horizontalAlignment = Alignment.CenterHorizontally,
        marginTop = BisqUIConstants.ScreenPadding,
        onDismissRequest = { onDismissOverlay() }
    ) {
        BisqText.h4Regular("mobile.resources.restore.password.headline".i18n(), color = BisqTheme.colors.primary)
        BisqGap.V2()
        BisqText.baseLight("mobile.resources.restore.password.info".i18n())
        BisqGap.V2()
        BisqTextField(
            value = password,
            label = "mobile.resources.restore.password".i18n(),
            onValueChange = { newValue, isValid ->
                password = newValue
            },
            isPasswordField = true,
        )
        BisqGap.V2()
        Column {
            BisqButton(
                text = "mobile.resources.restore.password.button".i18n(),
                onClick = { onPassword(password) },
                disabled = password.isEmpty(),
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = "mobile.resources.restore.password.button".i18n() },
            )
            BisqGap.VHalf()
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = { onDismissOverlay() },
                fullWidth = true,
                modifier = Modifier.semantics { contentDescription = "action.cancel".i18n() },
            )
        }
    }
}

@Composable
fun ErrorOverlay(
    errorMessage: String,
    onDismissRequest: () -> Unit = {},
) {
    BisqDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExclamationRedIcon()
            BisqGap.HQuarter()
            BisqText.h4Regular("mobile.genericError.headline".i18n())
        }

        BisqGap.V1()

        BisqText.baseLight(errorMessage)
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var fileName = "data.na".i18n()
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex != -1) {
            fileName = cursor.getString(nameIndex)
        }
    }
    return fileName
}
