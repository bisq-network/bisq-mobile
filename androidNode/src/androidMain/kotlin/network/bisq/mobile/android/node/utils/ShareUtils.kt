package network.bisq.mobile.android.node.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

fun shareBackup(context: Context, contentUriString: String, chooserTitle: String = "Share Bisq backup") {
    val uri = contentUriString.toUri()
    val clipData = ClipData.newUri(context.contentResolver, "Backup", uri)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        setClipData(clipData)
//        putExtra(Intent.EXTRA_STREAM, uri)
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
