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
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
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
import network.bisq.mobile.presentation.MainPresenter
import org.koin.compose.koinInject

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContract

private class OpenDocumentWithPersist : ActivityResultContract<Array<String>, Uri?>() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (input.isNotEmpty()) input.first() else "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

const val backupPrefix = "bisq2_mobile-backup-"
private const val MAX_BACKUP_SIZE_BYTES = 200L * 1024 * 1024

@Composable
actual fun RestoreBackup(onRestoreBackup: (String, String?, ByteArray) -> CompletableDeferred<String?>) {
    val context = LocalContext.current
    val presenter: MainPresenter = koinInject()

    val scope = rememberCoroutineScope()
    val log: Logger = remember { getLogger("ImportBackupFile") }
    var showRestoringOverlay: Boolean by remember { mutableStateOf(false) }

    val onRestoreUpdated by rememberUpdatedState(onRestoreBackup)

    var showPasswordOverlay: Boolean by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }

    var selectedFileName: String? by remember { mutableStateOf(null) }
    var selectedFileData: ByteArray? by remember { mutableStateOf(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = OpenDocumentWithPersist(),
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
                        if (size > MAX_BACKUP_SIZE_BYTES) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "mobile.resources.restore.error.fileSizeTooLarge".i18n()
                            }
                            return@launch
                        }

                        // Note: Reading entire file into memory can cause OOM on low-RAM devices
                        // The callback signature requires ByteArray, preventing streaming approach
                        // MAX_BACKUP_SIZE_BYTES provides some protection but may still be too large for some devices
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
                                val restore = onRestoreUpdated
                                showRestoringOverlay = true
                                val deferredErrorMessage: CompletableDeferred<String?> = restore(fileName, null, bytes)
                                scope.launch(Dispatchers.Main) {
                                    try {
                                        val result = deferredErrorMessage.await()
                                        if (result != null) {
                                            errorMessage = result
                                        } else {
                                            presenter.showSnackbar("mobile.resources.restore.success".i18n(), isError = false)
                                            showPasswordOverlay = false
                                        }
                                    } catch (t: Throwable) {
                                        errorMessage = t.message ?: t.toString().take(20)
                                    } finally {
                                        showRestoringOverlay = false
                                    }
                                }
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
                    showRestoringOverlay = true
                    val deferredErrorMessage: CompletableDeferred<String?> = onRestoreUpdated(fileName, password, data)
                    scope.launch(Dispatchers.Main) {
                        try {
                            val result = deferredErrorMessage.await()
                            if (result != null) {
                                errorMessage = result
                            } else {
                                presenter.showSnackbar("mobile.resources.restore.success".i18n(), isError = false)
                                showPasswordOverlay = false
                                selectedFileName = null
                                selectedFileData = null
                            }
                        } catch (t: Throwable) {
                            errorMessage = t.message ?: t.toString().take(20)
                        } finally {
                            showRestoringOverlay = false
                        }
                    }
                } else {
                    scope.launch(Dispatchers.Main) {
                        selectedFileName = null
                        selectedFileData = null
                    }
                }
            },
            onDismissOverlay = {
                scope.launch(Dispatchers.Main) {
                    showPasswordOverlay = false
                    selectedFileName = null
                    selectedFileData = null
                }
            }
        )
    }

    if (showRestoringOverlay) {
        RestoringOverlay()
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

@Composable
fun RestoringOverlay() {
    BisqDialog(
        horizontalAlignment = Alignment.CenterHorizontally,
        marginTop = BisqUIConstants.ScreenPadding,
        onDismissRequest = { /* non-dismissable while restoring */ }
    ) {
        CircularProgressIndicator()
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
