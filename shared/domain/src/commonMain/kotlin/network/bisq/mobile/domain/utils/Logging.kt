package network.bisq.mobile.domain.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.LoggerConfig
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import network.bisq.mobile.client.shared.BuildConfig

internal const val DEFAULT_LOGGER_TAG = "Default"

/**
 * Shared by every release logger. Immutable, so any thread may read it; loggers themselves are
 * created per access and never cached, which keeps this file free of shared mutable state.
 */
internal val releaseLoggerConfig: LoggerConfig by lazy {
    loggerConfigInit(
        RedactingLogWriter(platformLogWriter()),
        minSeverity = Severity.Error,
    )
}

interface Logging {
    val log: Logger
        get() {
            return getLogger(this)
        }
}

// Anonymous classes and lambdas have no simpleName
fun getLogger(anyObj: Any): Logger = getLogger(anyObj::class.simpleName ?: DEFAULT_LOGGER_TAG)

fun getLogger(tag: String): Logger = createLogger(tag, BuildConfig.IS_DEBUG)

/**
 * Creates a logger with appropriate configuration based on build type.
 *
 * Debug builds show every level with full stack traces: those logs stay on the developer's
 * machine. They use Kermit's global config, so platform setup such as the iOS Xcode writer
 * applies. Release builds show ERROR and ASSERT only, through [RedactingLogWriter], so a throwable
 * reaches the device log as its class chain and never as a stack trace carrying its messages.
 * Call sites still keep messages out of their own text (see `Throwable.redactedSummary`): the
 * writer cannot tell a message that was interpolated into the line from the line itself.
 */
internal fun createLogger(
    tag: String,
    isDebug: Boolean,
): Logger =
    if (isDebug) {
        Logger.withTag(tag)
    } else {
        Logger(config = releaseLoggerConfig, tag = tag)
    }
