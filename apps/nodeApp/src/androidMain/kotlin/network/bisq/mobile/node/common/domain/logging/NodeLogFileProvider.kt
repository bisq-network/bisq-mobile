package network.bisq.mobile.node.common.domain.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.share.AppLogFile
import network.bisq.mobile.presentation.common.share.AppLogFileProvider
import java.io.File

/**
 * Exposes the log file the embedded bisq2 core writes through logback
 * (`ApplicationService.setupLogging` -> `LogSetup`), which lives next to the core's data in the
 * app data dir. Rolled files (`bisq_1.log`, ...) are ignored: the current file holds what a fresh
 * bug report is about.
 */
class NodeLogFileProvider(
    private val appDataDir: File,
) : AppLogFileProvider {
    private val log = getLogger("NodeLogFileProvider")

    override suspend fun logFile(): AppLogFile? =
        withContext(Dispatchers.IO) {
            runCatching {
                File(appDataDir, LOG_FILE_NAME)
                    .takeIf { it.isFile && it.canRead() && it.length() > 0 }
                    ?.let { AppLogFile(path = it.absolutePath, name = it.name) }
            }.getOrElse { e ->
                log.w(e) { "Failed to access $LOG_FILE_NAME" }
                null
            }
        }

    private companion object {
        // bisq2 logs to <appDataDir>/bisq.log (LogSetup appends ".log" to the "bisq" base name).
        const val LOG_FILE_NAME = "bisq.log"
    }
}
