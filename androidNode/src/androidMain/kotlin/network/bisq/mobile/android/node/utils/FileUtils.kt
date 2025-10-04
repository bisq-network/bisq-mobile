package network.bisq.mobile.android.node.utils

import network.bisq.mobile.domain.utils.getLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun copyDirectory(
    sourceDir: File,
    destDir: File,
    excludedDirs: List<String>
) {
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    sourceDir.walkTopDown().forEach { src ->
        val relativePath = src.relativeTo(sourceDir).path
        if (relativePath.isEmpty()) return@forEach

        // Skip excluded directories
        if (excludedDirs.any { relativePath.startsWith(it) }) return@forEach

        val dest = File(destDir, relativePath)
        if (src.isDirectory) {
            dest.mkdirs()
        } else {
            src.copyTo(dest, overwrite = true)
        }
    }
}

fun zipDirectory(sourceDir: File, zipFile: File) {
    require(sourceDir.exists() && sourceDir.isDirectory) { "Source dir does not exist: ${sourceDir.absolutePath}" }

    zipFile.outputStream().use { fos ->
        ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
            val basePathLen = sourceDir.absolutePath.length
            sourceDir.walkTopDown().forEach { file ->
                val relativePath = file.absolutePath.substring(basePathLen).trimStart('/')
                if (file.isDirectory) {
                    if (relativePath.isNotEmpty()) {
                        val dirEntry = ZipEntry("$relativePath/")
                        zos.putNextEntry(dirEntry)
                        zos.closeEntry()
                    }
                } else if (file.isFile) {
                    val entry = ZipEntry(relativePath)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }
}

fun unzipToDirectory(inputStream: InputStream, targetDir: File) {
    ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
        var entry: ZipEntry? = zis.nextEntry
        val targetCanonical = targetDir.canonicalPath + File.separator

        while (entry != null) {
            val outFile = File(targetDir, entry.name)
            val outCanonical = outFile.canonicalPath

            // prevent ZipSlip
            if (!outCanonical.startsWith(targetCanonical)) {
                throw IOException("Illegal zip entry path: ${entry.name}")
            }

            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }
            }

            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

fun deleteFileInDirectory(targetDir: File, fileFilter: (File) -> Boolean = { true }) {
    if (!targetDir.exists() || !targetDir.isDirectory) return
    targetDir.listFiles()
        ?.filter { fileFilter.invoke(it) }
        ?.forEach { file ->
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
}

fun moveDirReplace(sourceDir: File, targetDir: File) {
    if (!sourceDir.exists() || !sourceDir.isDirectory) return

    // Delete target if it exists
    if (targetDir.exists()) {
        if (!targetDir.deleteRecursively()) {
            getLogger("moveDirReplace").w { "Could not delete $targetDir" }
        }
    }

    // Ensure parent directories exist
    targetDir.parentFile?.mkdirs()

    // Move by renaming if possible (fast), otherwise copy recursively
    if (!sourceDir.renameTo(targetDir)) {
        // fallback: copy recursively
        if (!sourceDir.copyRecursively(targetDir, overwrite = true)) {
            getLogger("moveDirReplace").w { "Could not copyRecursively $sourceDir to $targetDir" }
        }
        if (!sourceDir.deleteRecursively()) {
            getLogger("moveDirReplace").w { "Could not deleteRecursively $sourceDir" }
        }
    }
}
