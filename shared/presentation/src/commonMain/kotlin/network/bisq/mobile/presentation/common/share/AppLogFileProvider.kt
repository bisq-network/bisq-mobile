package network.bisq.mobile.presentation.common.share

/**
 * Access to a log file the app itself writes to disk, for bug reports.
 *
 * Only the node app has one: the embedded bisq2 core logs to `bisq.log` in the app data dir. The
 * client app writes no log file, so it binds [NoLogFileProvider] and bug reports fall back to the
 * error message and its stack trace.
 *
 * The raw file never leaves the device. [logFile] is the cheap descriptor the bug-report panel
 * shows; [prepareForSharing] produces the copy that is actually shared, redacted and capped, and
 * fails rather than falling back to the raw file when that copy cannot be produced.
 */
interface AppLogFileProvider {
    /** The log file to offer for sharing, or null when there is none. */
    suspend fun logFile(): AppLogFile?

    /**
     * Writes the shareable copy of the log and returns it. A failure means nothing may be shared;
     * callers must not share [logFile] instead.
     */
    suspend fun prepareForSharing(): Result<AppLogFile>
}

/**
 * A log file on disk. It is shared by copying the file itself rather than by reading it into
 * memory, because bisq2 rolls its log at 10 MB.
 */
data class AppLogFile(
    val path: String,
    val name: String,
)

object NoLogFileProvider : AppLogFileProvider {
    override suspend fun logFile(): AppLogFile? = null

    override suspend fun prepareForSharing(): Result<AppLogFile> = Result.failure(IllegalStateException("This app writes no log file"))
}
