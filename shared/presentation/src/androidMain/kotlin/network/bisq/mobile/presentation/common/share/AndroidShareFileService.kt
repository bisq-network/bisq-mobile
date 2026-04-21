package network.bisq.mobile.presentation.common.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import network.bisq.mobile.domain.utils.getLogger
import java.io.File

class AndroidShareFileService(
    private val context: Context,
) : ShareFileService {
    private val log = getLogger("AndroidShareFileService")

    override fun shareUtf8TextFile(
        content: String,
        fileName: String,
    ): Result<Unit> =
        try {
            val exportDir = File(context.cacheDir, "shared_files").apply { mkdirs() }
            val outFile = File(exportDir, fileName)
            outFile.writeText(content, Charsets.UTF_8)

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, outFile)

            // Use text/plain so the system resolver includes Files, Drive "Save to device",
            // Bluetooth, etc. Many handlers do not register for text/csv even though the
            // file name remains .csv and content is valid CSV.
            val clipData = ClipData.newUri(context.contentResolver, fileName, uri)
            val share =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setClipData(clipData)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val chooser =
                Intent.createChooser(share, fileName).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            log.e(e) { "Failed to share file" }
            Result.failure(e)
        }
}
