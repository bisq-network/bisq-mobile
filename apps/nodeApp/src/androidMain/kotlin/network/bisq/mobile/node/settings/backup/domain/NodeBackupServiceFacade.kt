package network.bisq.mobile.node.settings.backup.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import bisq.common.application.DevMode.isDevMode
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.decrypt
import network.bisq.mobile.domain.utils.encrypt
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.domain.service.NodeApplicationLifecycleService
import network.bisq.mobile.node.common.domain.utils.copyDirectory
import network.bisq.mobile.node.common.domain.utils.deleteFileInDirectory
import network.bisq.mobile.node.common.domain.utils.getShareableUriForFile
import network.bisq.mobile.node.common.domain.utils.saveToDownloads
import network.bisq.mobile.node.common.domain.utils.shareBackup
import network.bisq.mobile.node.common.domain.utils.unzipToDirectory
import network.bisq.mobile.node.common.domain.utils.zipDirectory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val MAX_BACKUP_SIZE_BYTES = 200L * 1024 * 1024
const val BACKUP_FILE_NAME = "bisq_db_from_backup"
const val BACKUP_PREFIX = "bisq2_mobile-backup-"

data class RestorePreFlightResult(
    val errorMessage: String? = null,
    val passwordRequired: Boolean = false,
)

class NodeBackupServiceFacade(
    private val nodeApplicationLifecycleService: NodeApplicationLifecycleService,
    private val context: Context,
) : ServiceFacade() {
    fun backupDataDir(password: String?): Deferred<Throwable?> {
        return serviceScope.async(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir
                val dataDir = File(context.filesDir, "Bisq2_mobile")
                val dbDir = File(dataDir, "db")
                val destDir = File(cacheDir, "bisq_db").apply { mkdirs() }
                // Dedicated share directory exposed via FileProvider (see file_paths.xml)
                val shareDir = File(cacheDir, "backups").apply { mkdirs() }

                // Copy db dir excluding cache and network_db sub dirs to context.cacheDir/bisq_db
                copyDirectory(
                    sourceDir = dbDir,
                    destDir = destDir,
                    excludedDirs = listOf("cache", "network_db"),
                )

                val zipFile = File.createTempFile("bisq-backup-", ".zip", cacheDir)
                zipDirectory(destDir, zipFile)
                destDir.deleteRecursively()

                // Clean up any previously exported backups in the share directory
                deleteFileInDirectory(
                    targetDir = shareDir,
                    fileFilter = { it.name.startsWith(BACKUP_PREFIX) },
                )

                val sanitizedPassword = password?.trim()?.takeIf { it.isNotEmpty() }
                val useEncryption = !sanitizedPassword.isNullOrEmpty()
                val outName = getCurrentBackupFileName(useEncryption)
                val outFile = File(shareDir, outName)
                try {
                    if (useEncryption) {
                        encrypt(zipFile, outFile, sanitizedPassword)
                        zipFile.delete()
                    } else if (!zipFile.renameTo(outFile)) {
                        zipFile.copyTo(outFile, overwrite = true)
                    }
                } catch (e: Exception) {
                    outFile.delete()
                    throw e
                } finally {
                    if (zipFile.exists()) {
                        zipFile.delete()
                    }
                }
                val uri = getShareableUriForFile(outFile, context)

                // In debug/dev mode, also save a copy to Downloads for easy local testing
                if (isDevMode()) {
                    try {
                        val mimeType =
                            if (useEncryption) "application/octet-stream" else "application/zip"
                        saveToDownloads(context, outFile, outName, mimeType)
                    } catch (t: Throwable) {
                        log.w(t) { "Failed to save backup to Downloads in debug mode" }
                    }
                }

                val shareMime = if (useEncryption) "application/octet-stream" else "application/zip"
                shareBackup(context, uri.toString(), mimeType = shareMime)
                return@async null
            } catch (e: Exception) {
                log.e(e) { "Failed to backup data directory" }
                return@async e
            }
        }
    }

    suspend fun restorePrefightCheck(uri: Uri): RestorePreFlightResult {
        return withContext(Dispatchers.IO) {
            try {
                // Persist access across restarts
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                log.e(e) { "takePersistableUriPermission failed" }
            }

            var passwordRequired = false
            var errorMessage: String?
            try {
                val fileName = getFileName(context, uri)
                passwordRequired = fileName.endsWith(".enc")

                val isValid =
                    fileName.startsWith(BACKUP_PREFIX) &&
                        (passwordRequired || fileName.endsWith(".zip"))

                if (!isValid) {
                    throw IllegalStateException("mobile.resources.restore.error.invalidFileName".i18n())
                }

                val size =
                    context.contentResolver
                        .openFileDescriptor(uri, "r")
                        .use { it?.statSize } ?: 0
                if (size > MAX_BACKUP_SIZE_BYTES) {
                    throw IllegalStateException("mobile.resources.restore.error.fileSizeTooLarge".i18n())
                } else if (size <= 0L) {
                    throw IllegalStateException("mobile.resources.restore.error.fileEmpty".i18n())
                }

                // we just read a few bytes to ensure we can read it
                val bytes =
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readNBytes(4)
                    }
                if (bytes == null || bytes.isEmpty()) {
                    throw IllegalStateException("mobile.resources.restore.error.cannotReadFile".i18n())
                }

                return@withContext RestorePreFlightResult(passwordRequired = passwordRequired)
            } catch (e: Exception) {
                log.e(e) { "Importing backup failed" }
                errorMessage = "mobile.resources.restore.error".i18n(e.message ?: e.toString())
                return@withContext RestorePreFlightResult(
                    errorMessage = errorMessage,
                    passwordRequired = passwordRequired,
                )
            }
        }
    }

    fun restoreBackup(
        uri: Uri,
        password: String?,
        view: Any?,
    ): Deferred<Throwable?> {
        return serviceScope.async(Dispatchers.IO) {
            var inputStream: InputStream? = null
            try {
                val filesDir = context.filesDir

                val backupDir = File(filesDir, BACKUP_FILE_NAME)
                if (backupDir.exists()) backupDir.deleteRecursively()

                inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("mobile.resources.restore.error.cannotReadFile".i18n())

                var decryptedTempFile: File? = null
                if (!password.isNullOrEmpty()) {
                    try {
                        val decryptedFile =
                            inputStream.use {
                                decrypt(it, password)
                            }
                        decryptedTempFile = decryptedFile
                        inputStream = decryptedFile.inputStream()
                    } catch (e: Exception) {
                        val errorMessage = "mobile.resources.restore.error.decryptionFailed".i18n()
                        throw GeneralSecurityException(errorMessage, e)
                    }
                }
                try {
                    inputStream.use {
                        unzipToDirectory(it, backupDir)
                    }
                } catch (e: Exception) {
                    // Clean up incomplete backup to prevent corrupted restore on next launch
                    if (backupDir.exists()) {
                        backupDir.deleteRecursively()
                    }
                    val errorMessage = "mobile.resources.restore.error.unzipFailed".i18n()
                    throw IOException(errorMessage, e)
                } finally {
                    decryptedTempFile?.let { temp ->
                        if (!temp.delete()) {
                            temp.deleteOnExit()
                        }
                    }
                }

                if (backupDir.exists()) {
                    val requiredDirs =
                        listOf(File(backupDir, "private"), File(backupDir, "settings"))
                    if (!requiredDirs.all { it.exists() && it.isDirectory }) {
                        val errorMessage =
                            "mobile.resources.restore.error.invalidBackupStructure".i18n()
                        throw IOException(errorMessage)
                    }

                    // Delay restart slightly so the UI can surface the success toast in beforeRestartHook
                    // before the process restarts.
                    // 1500ms chosen as a pragmatic balance between user feedback and flow speed.
                    delay(1500)
                    nodeApplicationLifecycleService.restartForRestoreDataDirectory(view) // onRestoreDataDir is called from UI, so view is not null here
                } else {
                    val errorMessage = "mobile.resources.restore.error.missingBackupDir".i18n()
                    throw IOException(errorMessage)
                }
                return@async null
            } catch (e: Exception) {
                log.e(e) { errorMessage(e) }
                return@async e
            }
        }
    }

    private fun errorMessage(e: Exception): String = e.message ?: e.javaClass.simpleName

    private fun getCurrentBackupFileName(useEncryption: Boolean): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val date = LocalDateTime.now().format(formatter)
        val postFix = if (useEncryption) ".enc" else ".zip"
        return BACKUP_PREFIX + date + postFix
    }

    private fun getFileName(
        context: Context,
        uri: Uri,
    ): String {
        var fileName = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}
