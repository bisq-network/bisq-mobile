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
    val logger = getLogger("unzipToDirectory")

    val allowedTopLevel = setOf("private", "settings")
    val MAX_TOTAL_UNCOMPRESSED_BYTES = 200L * 1024 * 1024 // 200 MiB
    val MAX_ENTRY_UNCOMPRESSED_BYTES = 50L * 1024 * 1024  // 50 MiB per file
    val MAX_ENTRIES = 10_000
    val MAX_DEPTH = 10
    val MAX_COMPRESSION_RATIO = 200.0 // uncompressed/compressed

    ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
        var entry: ZipEntry? = zis.nextEntry
        val targetCanonical = targetDir.canonicalPath + File.separator
        var totalUncompressedBytes = 0L
        var entryCount = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        while (entry != null) {
            entryCount += 1
            if (entryCount > MAX_ENTRIES) {
                throw IOException("Zip contains too many entries")
            }

            val name = entry.name

            // Basic name checks
            if (name.startsWith('/') || name.contains("..")) {
                throw IOException("Illegal zip entry path: $name")
            }

            // Enforce allowed top-level directories only
            val topLevel = name.substringBefore('/')
            if (topLevel.isNotEmpty() && topLevel !in allowedTopLevel) {
                throw IOException("Disallowed top-level entry: $topLevel")
            }

            // Depth limit
            val depth = name.count { it == '/' }
            if (depth > MAX_DEPTH) {
                throw IOException("Zip entry too deep: $name")
            }

            val outFile = File(targetDir, name)
            val outCanonical = outFile.canonicalPath

            // Prevent ZipSlip by canonical path check relative to target
            if (!outCanonical.startsWith(targetCanonical)) {
                throw IOException("Illegal zip entry path: $name")
            }

            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                var entryBytes = 0L
                FileOutputStream(outFile).use { fos ->
                    var read = zis.read(buffer)
                    while (read > 0) {
                        entryBytes += read
                        totalUncompressedBytes += read

                        if (entryBytes > MAX_ENTRY_UNCOMPRESSED_BYTES) {
                            fos.flush()
                            outFile.delete()
                            throw IOException("Zip entry too large: $name")
                        }
                        if (totalUncompressedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                            fos.flush()
                            outFile.delete()
                            throw IOException("Zip content exceeds maximum allowed size")
                        }

                        fos.write(buffer, 0, read)
                        read = zis.read(buffer)
                    }
                }

                val compressedSize = entry.compressedSize
                if (compressedSize > 0) {
                    val ratio = entryBytes.toDouble() / compressedSize.toDouble()
                    if (ratio > MAX_COMPRESSION_RATIO) {
                        outFile.delete()
                        throw IOException("Suspicious compression ratio for entry: $name")
                    }
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
