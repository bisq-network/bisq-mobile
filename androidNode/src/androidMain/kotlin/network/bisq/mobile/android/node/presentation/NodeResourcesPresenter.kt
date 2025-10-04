package network.bisq.mobile.android.node.presentation

import android.content.Context
import network.bisq.mobile.android.node.NodeApplicationLifecycleService
import network.bisq.mobile.android.node.utils.copyDirectory
import network.bisq.mobile.android.node.utils.decrypt
import network.bisq.mobile.android.node.utils.deleteFileInDirectory
import network.bisq.mobile.android.node.utils.encrypt
import network.bisq.mobile.android.node.utils.getShareableUriForFile
import network.bisq.mobile.android.node.utils.shareBackup
import network.bisq.mobile.android.node.utils.unzipToDirectory
import network.bisq.mobile.android.node.utils.zipDirectory
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
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

                // Copy db dir excluding cache and network_db sub dirs to context.cacheDir/bisq_db
                copyDirectory(
                    sourceDir = dbDir,
                    destDir = destDir,
                    excludedDirs = listOf("cache", "network_db")
                )

                val zipFile = File.createTempFile("bisq-backup-", ".zip", cacheDir)
                zipDirectory(destDir, zipFile)
                destDir.deleteRecursively()

                deleteFileInDirectory(targetDir = cacheDir, fileFilter = { it.name.startsWith(backupPrefix) })

                val useEncryption = !password.isNullOrEmpty()
                val outName = getCurrentBackupFileName(useEncryption)
                val outFile = File(cacheDir, outName)

                if (useEncryption) {
                    encrypt(zipFile, outFile, password)
                    zipFile.delete()
                } else {
                    zipFile.renameTo(outFile)
                }

                val uri = getShareableUriForFile(outFile, context)

                shareBackup(context, uri.toString())
            } catch (e: Exception) {
                log.e(e) { "Failed to backup data directory" }
            }
        }
    }

    override fun onRestoreDataDir(fileName: String, password: String?, data: ByteArray) {
        val context: Context by inject()
        launchIO {
            try {
                val filesDir = context.filesDir

                val backupDir = File(filesDir, backupFileName)
                if (backupDir.exists()) backupDir.deleteRecursively()
                val rawInputStream: InputStream = ByteArrayInputStream(data)

                val inputStream: InputStream = if (!password.isNullOrEmpty()) {
                    decrypt(rawInputStream, password)
                } else {
                    rawInputStream
                }

                unzipToDirectory(inputStream, backupDir)
                inputStream.close()

                if (backupDir.exists()) {
                    nodeApplicationLifecycleService.restartForRestoreDataDirectory(context)
                } else {
                    log.e { "onRestoreDataDir: $backupDir does not exits" }
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to backup data directory" }
            }
        }
    }

    private fun getCurrentBackupFileName(useEncryption: Boolean): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val date = LocalDateTime.now().format(formatter)
        val postFix = if (useEncryption) ".enc" else ".zip"
        return backupPrefix + date + postFix
    }
}