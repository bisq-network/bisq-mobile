package network.bisq.mobile.node.common.domain.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.logging.LogScrubber
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.domain.utils.redactedSummary
import network.bisq.mobile.presentation.common.share.AppLogFile
import network.bisq.mobile.presentation.common.share.AppLogFileProvider
import java.io.File
import kotlin.math.max

/**
 * Exposes the log file the embedded bisq2 core writes through logback
 * (`ApplicationService.setupLogging` -> `LogSetup`), which lives next to the core's data in the
 * app data dir. Rolled files (`bisq_1.log`, ...) are ignored: the current file holds what a fresh
 * bug report is about.
 *
 * What is shared is never that file. [prepareForSharing] writes a redacted copy of its tail into
 * [exportDir] and hands that out; see [SHARED_LOG_TAIL_BYTES] and [LogScrubber] for the two
 * mobile-specific choices. bisq2's own log masking (`LogMaskConverter`) only hides the home
 * directory and is a no-op on Android, so the scrubbing has to happen here, at the point where
 * the file leaves the device.
 */
class NodeLogFileProvider(
    private val appDataDir: File,
    private val exportDir: File,
) : AppLogFileProvider {
    private val log = getLogger("NodeLogFileProvider")

    override suspend fun logFile(): AppLogFile? =
        withContext(Dispatchers.IO) {
            runCatching {
                rawLogFile()
                    ?.let { AppLogFile(path = it.absolutePath, name = it.name) }
            }.getOrElse { e ->
                log.w(e) { "Failed to access $LOG_FILE_NAME" }
                null
            }
        }

    override suspend fun prepareForSharing(): Result<AppLogFile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = rawLogFile() ?: error("No log file to share")
                val target = File(File(exportDir, EXPORT_DIR_NAME), SHARED_LOG_FILE_NAME)
                target.parentFile?.mkdirs()
                target.delete()
                writeRedactedTail(source, target)
                AppLogFile(path = target.absolutePath, name = target.name)
            }.onFailure { e -> log.e { "Failed to prepare the log for sharing: ${e.redactedSummary()}" } }
        }

    private fun rawLogFile(): File? =
        File(appDataDir, LOG_FILE_NAME)
            .takeIf { it.isFile && it.canRead() && it.length() > 0 }

    /**
     * Streams the last [SHARED_LOG_TAIL_BYTES] of [source] through a fresh [LogScrubber] into
     * [target], starting at the first complete line, with a header saying what the reader is
     * looking at and a footer with the redaction counts. Streamed, so a 10 MB log never sits in
     * memory, and written to a separate file, so the raw log stays untouched.
     */
    private fun writeRedactedTail(
        source: File,
        target: File,
    ) {
        val scrubber = LogScrubber()
        val start = max(0L, source.length() - SHARED_LOG_TAIL_BYTES)
        target.bufferedWriter().use { out ->
            out.write("# ${source.name}, redacted for sharing by Bisq Easy Node.")
            out.newLine()
            out.write(
                "# Onion hosts, ids, nyms, profile text and key material are replaced by placeholders like <onion#3>; " +
                    "the same placeholder always stands for the same value within this file. " +
                    if (start > 0) "Only the most recent ${SHARED_LOG_TAIL_BYTES / (1024 * 1024)} MB are included." else "The whole file is included.",
            )
            out.newLine()
            source.inputStream().use { input ->
                input.skip(start)
                input.bufferedReader().useLines { lines ->
                    // Skipping into the file lands mid-line; the partial first line is dropped.
                    val complete = if (start > 0) lines.drop(1) else lines
                    complete.forEach { line ->
                        out.write(scrubber.scrubLine(line))
                        out.newLine()
                    }
                }
            }
            out.write("# redacted: ${scrubber.summaryLine()}")
            out.newLine()
        }
    }

    companion object {
        // bisq2 logs to <appDataDir>/bisq.log (LogSetup appends ".log" to the "bisq" base name).
        const val LOG_FILE_NAME = "bisq.log"

        /** Named so a maintainer sees at a glance that the attachment is not the raw log. */
        const val SHARED_LOG_FILE_NAME = "bisq-node-redacted.log"

        const val EXPORT_DIR_NAME = "redacted_logs"

        /**
         * Mobile tweak: bisq2 rolls `bisq.log` at 10 MB, but a bug report is about what just
         * happened, and scrubbing runs on the phone right before the share sheet opens. 2 MB is
         * roughly 15k lines, more than a session, and keeps that wait around a second.
         */
        const val SHARED_LOG_TAIL_BYTES = 2L * 1024 * 1024
    }
}
