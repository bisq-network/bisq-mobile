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
    require(sourceDir.exists() && sourceDir.isDirectory) { "Source dir does not exist or is not a directory: ${sourceDir.absolutePath}" }
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
    require(sourceDir.exists() && sourceDir.isDirectory) { "Source dir does not exist or is not a directory: ${sourceDir.absolutePath}" }

    val logger = getLogger("moveDirReplace")
    logger.i { "KMP moveDirReplace: start source='${sourceDir.absolutePath}' target='${targetDir.absolutePath}'" }

    val parent = targetDir.parentFile ?: throw IOException("Target has no parent: ${targetDir.absolutePath}")
    if (!parent.exists()) parent.mkdirs()

    val tempOld = File(parent, "${targetDir.name}.old")
    if (tempOld.exists() && !tempOld.deleteRecursively()) {
        throw IOException("Cannot clear temp backup: ${tempOld.absolutePath}")
    }

    var hadOld = false
    if (targetDir.exists()) {
        hadOld = true
        logger.i { "KMP moveDirReplace: backing up existing target to '${tempOld.absolutePath}'" }
        if (!targetDir.renameTo(tempOld)) {
            logger.i { "KMP moveDirReplace: rename backup failed, falling back to copy+delete" }
            if (!targetDir.copyRecursively(tempOld, overwrite = true)) {
                throw IOException("Cannot backup existing target: ${targetDir.absolutePath}")
            }
            if (!targetDir.deleteRecursively()) {
                // Cleanup copied backup to avoid leaving stale temp if we failed to remove original target
                tempOld.deleteRecursively()
                throw IOException("Cannot remove existing target: ${targetDir.absolutePath}")
            }
        }
    }

    try {
        logger.i { "KMP moveDirReplace: replacing target with source" }
        if (!sourceDir.renameTo(targetDir)) {
            logger.i { "KMP moveDirReplace: rename replace failed, falling back to copy+delete" }
            if (!sourceDir.copyRecursively(targetDir, overwrite = true)) {
                throw IOException("Cannot copy source to target: ${sourceDir.absolutePath} -> ${targetDir.absolutePath}")
            }
            if (!sourceDir.deleteRecursively()) {
                // Rollback: remove partial target and restore old content if present
                logger.w { "KMP moveDirReplace: delete source after copy failed; rolling back" }
                targetDir.deleteRecursively()
                if (hadOld && tempOld.exists()) {
                    if (!tempOld.renameTo(targetDir)) {
                        if (!tempOld.copyRecursively(targetDir, overwrite = true) || !tempOld.deleteRecursively()) {
                            logger.w { "KMP moveDirReplace: rollback left temp at ${tempOld.absolutePath}" }
                        }
                    }
                }
                throw IOException("Cannot remove source after copy: ${sourceDir.absolutePath}")
            }
        }
        logger.i { "KMP moveDirReplace: replace succeeded" }
    } catch (e: Exception) {
        // General rollback on failure
        logger.w(e) { "KMP moveDirReplace: failure; rolling back" }
        targetDir.deleteRecursively()
        if (hadOld && tempOld.exists()) {
            if (!tempOld.renameTo(targetDir)) {
                if (!tempOld.copyRecursively(targetDir, overwrite = true) || !tempOld.deleteRecursively()) {
                    logger.w { "KMP moveDirReplace: rollback left temp at ${tempOld.absolutePath}" }
                }
            }
        }
        throw if (e is IOException) e else IOException(e.message ?: "moveDirReplace failed", e)
    } finally {
        if (tempOld.exists()) {
            if (!tempOld.deleteRecursively()) {
                logger.w { "KMP moveDirReplace: could not delete temp backup: ${tempOld.absolutePath}" }
            }
        }
    }
}
