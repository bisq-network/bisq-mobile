package network.bisq.mobile.presentation.common.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.utils.getLogger
import java.io.File

class AndroidShareFileService(
    private val context: Context,
) : ShareFileService {
    private val log = getLogger("AndroidShareFileService")

    override suspend fun shareUtf8TextFile(
        content: String,
        fileName: String,
    ): Result<Unit> =
        try {
            val sanitizedName = sanitizeShareFileBasename(fileName)
            val uri: Uri =
                withContext(Dispatchers.IO) {
                    val exportDir = File(context.cacheDir, "shared_files").apply { mkdirs() }
                    val outFile = File(exportDir, sanitizedName)
                    outFile.writeText(content, Charsets.UTF_8)
                    ensureShareOutputContainedIn(outFile, exportDir)

                    val authority = "${context.packageName}.fileprovider"
                    FileProvider.getUriForFile(context, authority, outFile)
                }

            withContext(Dispatchers.Main) {
                // Use text/plain so the system resolver includes Files, Drive "Save to device",
                // Bluetooth, etc. Many handlers do not register for text/csv even though the
                // file name remains .csv and content is valid CSV.
                val clipData = ClipData.newUri(context.contentResolver, sanitizedName, uri)
                val share =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setClipData(clipData)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                val chooser =
                    Intent.createChooser(share, sanitizedName).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                context.startActivity(chooser)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to share file" }
            Result.failure(e)
        }

    private companion object {
        const val DEFAULT_SHARE_BASENAME = "shared_export.txt"

        private val INVALID_FILENAME_CHARS = Regex("""[\\/:*?"<>|\x00-\x1F]""")

        /**
         * Produces a single path segment safe for [File] under a fixed parent: no separators,
         * no "." / ".." traversal, and no characters unsafe for common filesystems / share UIs.
         */
        fun sanitizeShareFileBasename(fileName: String): String {
            val normalized = fileName.replace('\\', '/').trim()
            if (normalized.isEmpty()) return DEFAULT_SHARE_BASENAME

            val parts = normalized.split('/').filter { it.isNotEmpty() }
            val candidate =
                parts.asReversed().firstOrNull { it != "." && it != ".." }
                    ?: DEFAULT_SHARE_BASENAME

            val cleaned =
                INVALID_FILENAME_CHARS
                    .replace(candidate, "_")
                    .trim('.', ' ')
                    .ifEmpty { DEFAULT_SHARE_BASENAME }

            return cleaned
        }

        fun ensureShareOutputContainedIn(
            file: File,
            exportDir: File,
        ) {
            val dir = exportDir.canonicalFile
            val dirPrefix = dir.path + File.separator
            val resolved = file.canonicalFile.path
            if (!resolved.startsWith(dirPrefix)) {
                file.delete()
                throw SecurityException("Share file path escapes export directory")
            }
        }
    }
}
