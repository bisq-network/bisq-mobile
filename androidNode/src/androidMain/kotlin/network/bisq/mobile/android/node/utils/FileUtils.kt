package network.bisq.mobile.android.node.utils

import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
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

fun zipDirectory(
    sourceDir: File,
    zipFile: File
) {
    if (!sourceDir.exists() || !sourceDir.isDirectory) {
        throw IllegalArgumentException("Source dir does not exist: ${sourceDir.absolutePath}")
    }

    zipFile.outputStream().use { fos ->
        ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
            val basePathLen = sourceDir.absolutePath.length// + 1
            sourceDir.walkTopDown().forEach { file ->
                val absolutePath = file.absolutePath
                val substring = absolutePath.substring(basePathLen)
                val oldChar = File.separatorChar
                val entryName = substring.replace(oldChar, '/')
                if (file.isDirectory) {
                    if (entryName.isNotEmpty()) {
                        val dirEntry = ZipEntry("$entryName/")
                        zos.putNextEntry(dirEntry)
                        zos.closeEntry()
                    }
                } else if (file.isFile) {
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    val res = sourceDir.deleteRecursively()
    val ress = sourceDir.delete()
}