package network.bisq.mobile.android.node.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun backupDataDir(context: Context, password: String?) {
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

    // We delete any potential left over files as we use diff. postfixes we check for both
    deleteFiles(cacheDir, true)
    deleteFiles(cacheDir, false)

    val useEncryption = !password.isNullOrEmpty()
    val outName = getCurrentBackupFileName(useEncryption)
    val outFile = File(cacheDir, outName)

    if (useEncryption) {
        encryptFileAesGcm(zipFile, outFile, password)
        zipFile.delete()
    } else {
        zipFile.renameTo(outFile)
    }

    val uri = getShareableUriForFile(outFile, context)
    val result = ExportResult(outFile.name, uri.toString())

    shareBackup(context, result.contentUriString)
}

fun deleteFiles(cacheDir: File, useEncryption: Boolean) {
    val outName = getCurrentBackupFileName(useEncryption)
    val outFile = File(cacheDir, outName)
    if (outFile.exists()) outFile.delete()
}

fun getCurrentBackupFileName(useEncryption: Boolean): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    val date = LocalDateTime.now().format(formatter)
    val postFix = if (useEncryption) ".enc" else ".zip"
    return "bisq2_mobile-backup-" + date + postFix
}

fun shareBackup(context: Context, contentUriString: String, chooserTitle: String = "Share Bisq backup") {
    val uri = contentUriString.toUri()
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(share, chooserTitle)
        .apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(chooser)
}

fun getShareableUriForFile(file: File, context: Context): Uri {
    // FileProvider authority must match your manifest/provider setup
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}

data class ExportResult(
    val fileName: String,
    val contentUriString: String // platform-specific string (on Android content:// URI)
)
