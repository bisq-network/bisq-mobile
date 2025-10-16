package network.bisq.mobile.android.node.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

import androidx.core.net.toUri
import java.io.File

fun shareBackup(context: Context, contentUriString: String, chooserTitle: String = "Share Bisq backup", mimeType: String? = null) {
    val uri = contentUriString.toUri()
    val clipData = ClipData.newUri(context.contentResolver, "Backup", uri)
    val actualMime = mimeType ?: "application/octet-stream"
    val share = Intent(Intent.ACTION_SEND).apply {
        type = actualMime
        setClipData(clipData)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Some apps (e.g., Gmail) behave better with explicit data + type
        setDataAndType(uri, actualMime)
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

fun saveToDownloads(context: Context, srcFile: File, displayName: String, mimeType: String = "application/octet-stream"): Uri? {
    return try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Scoped storage not available; skip in older devices
            return null
        }
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, contentValues) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            srcFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: return null
        // Mark as complete
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        uri
    } catch (t: Throwable) {
        null
    }
}
