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

fun getLogger(tag: String): Logger {
    return doGetLogger(tag)
}

private fun doGetLogger(tag: String?): Logger {
    return if (tag != null) {
        loggerCache.getOrPut(tag) { createLogger(tag) }
    } else {
        // Anonymous classes or lambda expressions do not provide a simpleName
        loggerCache.getOrPut("Default") { createLogger("Default") }
    }
}

/**
 * Creates a logger with appropriate configuration based on build type.
 * In release builds, only ERROR and ASSERT logs are shown.
 * In debug builds, all log levels are shown.
 */
private fun createLogger(tag: String): Logger {
    return if (BuildConfig.IS_DEBUG) {
        // Debug build: show all logs
        Logger.withTag(tag)
    } else {
        // Release build: only show ERROR and ASSERT logs
        Logger(
            config = loggerConfigInit(
                platformLogWriter(),
                minSeverity = Severity.Error
            ),
            tag = tag
        )
    }
}