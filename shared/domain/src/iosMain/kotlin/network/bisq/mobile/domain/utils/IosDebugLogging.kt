package network.bisq.mobile.domain.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Message
import co.touchlab.kermit.OSLogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import co.touchlab.kermit.XcodeSeverityWriter
import network.bisq.mobile.client.shared.BuildConfig
import platform.darwin.os_log_type_t

/**
 * Debug builds only: writes log text as public, so the device log (Console.app, pymobiledevice3)
 * shows it without Xcode attached. Without this, iOS replaces every Kermit message with
 * `<private>` once the debugger is detached. Release builds keep the ERROR-only, redacted
 * logging from [getLogger] and are not affected.
 */
fun useReadableDeviceLogsInDebugBuilds() {
    if (BuildConfig.IS_DEBUG) {
        Logger.setLogWriters(PublicXcodeSeverityWriter())
    }
}

/** [XcodeSeverityWriter] output, logged with `%{public}s`. */
private class PublicXcodeSeverityWriter : OSLogWriter(publicLogging = true) {
    private val xcodeWriter = XcodeSeverityWriter()

    override fun formatMessage(
        severity: Severity,
        tag: Tag,
        message: Message,
    ): String = xcodeWriter.formatMessage(severity, tag, message)

    override fun logThrowable(
        osLogSeverity: os_log_type_t,
        throwable: Throwable,
    ) = xcodeWriter.logThrowable(osLogSeverity, throwable)
}
