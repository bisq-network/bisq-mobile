package network.bisq.mobile.domain.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import network.bisq.mobile.client.shared.BuildConfig

private val loggerCache = mutableMapOf<String, Logger>()

interface Logging {
    val log: Logger
        get() {
            return getLogger(this)
        }
}

fun getLogger(anyObj: Any): Logger {
    val tag = anyObj::class.simpleName
    return doGetLogger(tag)
}

fun getLogger(tag: String): Logger = doGetLogger(tag)

private fun doGetLogger(tag: String?): Logger =
    if (tag != null) {
        loggerCache.getOrPut(tag) { createLogger(tag) }
    } else {
        // Anonymous classes or lambda expressions do not provide a simpleName
        loggerCache.getOrPut("Default") { createLogger("Default") }
    }

/**
 * Creates a logger with appropriate configuration based on build type.
 *
 * Debug builds show every level with full stack traces: those logs stay on the developer's
 * machine. Release builds show ERROR and ASSERT only, through [RedactingLogWriter], so a throwable
 * reaches the device log as its class chain and never as a stack trace carrying its messages.
 * Call sites still keep messages out of their own text (see `Throwable.redactedSummary`): the
 * writer cannot tell a message that was interpolated into the line from the line itself.
 */
private fun createLogger(tag: String): Logger =
    if (BuildConfig.IS_DEBUG) {
        Logger.withTag(tag)
    } else {
        Logger(
            config =
                loggerConfigInit(
                    RedactingLogWriter(platformLogWriter()),
                    minSeverity = Severity.Error,
                ),
            tag = tag,
        )
    }
