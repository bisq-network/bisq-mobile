package network.bisq.mobile.android.node.presentation

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import network.bisq.mobile.android.node.NodeApplicationLifecycleService
import network.bisq.mobile.android.node.utils.copyDirectory
import network.bisq.mobile.domain.utils.decrypt
import network.bisq.mobile.android.node.utils.deleteFileInDirectory
import network.bisq.mobile.domain.utils.encrypt
import network.bisq.mobile.android.node.utils.getShareableUriForFile
import network.bisq.mobile.android.node.utils.shareBackup
import network.bisq.mobile.android.node.utils.unzipToDirectory
import network.bisq.mobile.android.node.utils.zipDirectory
import network.bisq.mobile.android.node.utils.saveToDownloads
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val backupFileName = "bisq_db_from_backup"
const val backupPrefix = "bisq2_mobile-backup-"

class NodeResourcesPresenter(
    private val mainPresenter: MainPresenter,
    versionProvider: VersionProvider,
    deviceInfoProvider: DeviceInfoProvider,
    private val nodeApplicationLifecycleService: NodeApplicationLifecycleService
) : ResourcesPresenter(mainPresenter, versionProvider, deviceInfoProvider) {

    override fun onViewAttached() {
        super.onViewAttached()

        _showBackupAndRestore.value = true
    }

    override fun onBackupDataDir(password: String?) {
        _showBackupOverlay.value = false
        val context: Context by inject()
        launchIO {
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
                    excludedDirs = listOf("cache", "network_db")
                )

                val zipFile = File.createTempFile("bisq-backup-", ".zip", cacheDir)
                zipDirectory(destDir, zipFile)
                destDir.deleteRecursively()

                // Clean up any previously exported backups in the share directory
                deleteFileInDirectory(targetDir = shareDir, fileFilter = { it.name.startsWith(backupPrefix) })

                val sanitizedPassword = password?.trim()?.takeIf { it.isNotEmpty() }
                val useEncryption = !sanitizedPassword.isNullOrEmpty()
                val outName = getCurrentBackupFileName(useEncryption)
                val outFile = File(shareDir, outName)
                try {
                    if (useEncryption) {
                        // Run CPU-heavy PBKDF2/AES on Default to keep IO threads responsive
                        withContext(Dispatchers.Default) {
                            encrypt(zipFile, outFile, sanitizedPassword)
                        }
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

                mainPresenter.showSnackbar("mobile.resources.backup.success".i18n(), isError = false)

                // In debug/dev mode, also save a copy to Downloads for easy local testing
                if (isDevMode()) {
                    try {
                        val mimeType = if (useEncryption) "application/octet-stream" else "application/zip"
                        val saved = saveToDownloads(context, outFile, outName, mimeType)
                        if (saved != null) {
                            mainPresenter.showSnackbar("Saved to Downloads (debug): $outName", isError = false)
                        }
                    } catch (t: Throwable) {
                        log.w(t) { "KMP: Failed to save backup to Downloads in debug mode" }
                    }
                }

                val shareMime = if (useEncryption) "application/octet-stream" else "application/zip"
                shareBackup(context, uri.toString(), mimeType = shareMime)
            } catch (e: Exception) {
                log.e(e) { "Failed to backup data directory" }
            }
        }
    }

    override fun onRestoreDataDir(fileName: String, password: String?, data: ByteArray): CompletableDeferred<String?> {
        val context: Context by inject()
        val result: CompletableDeferred<String?> = CompletableDeferred()
        launchIO {
            try {
                val filesDir = context.filesDir

                val backupDir = File(filesDir, backupFileName)
                if (backupDir.exists()) backupDir.deleteRecursively()
                val rawInputStream: InputStream = ByteArrayInputStream(data)
                var decryptedTempFile: File? = null
                val inputStream: InputStream = if (!password.isNullOrEmpty()) {
                    try {
                        // Run CPU-heavy PBKDF2/AES on Default to keep IO threads responsive
                        val decryptedFile = withContext(Dispatchers.Default) {
                            decrypt(rawInputStream, password)
                        }
                        decryptedTempFile = decryptedFile
                        decryptedFile.inputStream()
                    } catch (e: Exception) {
                        val errorMessage = "mobile.resources.restore.error.decryptionFailed".i18n()
                        throw GeneralSecurityException(errorMessage, e)
                    }
                } else {
                    rawInputStream
                }
                try {
                    unzipToDirectory(inputStream, backupDir)
                } catch (e: Exception) {
                    // Clean up incomplete backup to prevent corrupted restore on next launch
                    if (backupDir.exists()) {
                        backupDir.deleteRecursively()
                    }
                    val errorMessage = "mobile.resources.restore.error.unzipFailed".i18n()
                    throw IOException(errorMessage, e)
                } finally {
                    try {
                        inputStream.close()
                    } catch (ignore: Exception) {
                    }
                    decryptedTempFile?.let { temp ->
                        if (!temp.delete()) {
                            temp.deleteOnExit()
                        }
                    }
                }

                if (backupDir.exists()) {
                    val requiredDirs = listOf(File(backupDir, "private"), File(backupDir, "settings"))
                    if (!requiredDirs.all { it.exists() && it.isDirectory }) {
                        val errorMessage = "mobile.resources.restore.error.invalidBackupStructure".i18n()
                        throw IOException(errorMessage)
                    }

                    // Delay restart slightly so the UI can surface the success toast before the process restarts.
                    // 1500ms chosen as a pragmatic balance between user feedback and flow speed.
                    result.complete(null)
                    delay(1500)
                    nodeApplicationLifecycleService.restartForRestoreDataDirectory(context)
                } else {
                    val errorMessage = "mobile.resources.restore.error.missingBackupDir".i18n()
                    throw IOException(errorMessage)
                }
            } catch (e: Exception) {
                log.e(e) { errorMessage(e) }
                result.completeExceptionally(e)
            }
        }
        return result
    }

    private fun errorMessage(e: Exception): String = e.message ?: e.javaClass.simpleName

    private fun getCurrentBackupFileName(useEncryption: Boolean): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val date = LocalDateTime.now().format(formatter)
        val postFix = if (useEncryption) ".enc" else ".zip"
        return backupPrefix + date + postFix
    }
}