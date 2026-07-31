package network.bisq.mobile.node.common.domain.logging

import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase

/**
 * Routes bisq2 core (slf4j/logback) log events into Android logcat with proper priorities.
 *
 * Everything is emitted under the single fixed [TAG] so devs can isolate the whole core with one
 * logcat filter (`adb logcat -s Bisq2`); the originating logger's short name is prefixed to the
 * message instead of the tag (pre-API-26 tags are length-limited, and per-logger tags would make
 * "show me everything from the core" filtering harder, not easier).
 *
 * Why an appender instead of letting the jar-bundled logback.xml ConsoleAppender print to stdout
 * (issue #767): the console route dies in SystemOutFilter (logback writes via OutputStream.write,
 * see there), and even unfiltered it would reach logcat as ANSI-escape noise at Info priority under
 * the generic "System.out" tag. Logcat is the native sink; this writes to it directly.
 */
class Bisq2LogcatAppender : AppenderBase<ILoggingEvent>() {
    companion object {
        const val TAG = "Bisq2"
    }

    override fun append(event: ILoggingEvent) {
        val message =
            buildString {
                append('[')
                append(shortLoggerName(event.loggerName))
                append("] ")
                append(event.formattedMessage)
                event.throwableProxy?.let {
                    append('\n')
                    append(ThrowableProxyUtil.asString(it))
                }
            }
        when (event.level.toInt()) {
            Level.ERROR_INT -> Log.e(TAG, message)
            Level.WARN_INT -> Log.w(TAG, message)
            Level.INFO_INT -> Log.i(TAG, message)
            Level.DEBUG_INT -> Log.d(TAG, message)
            else -> Log.v(TAG, message)
        }
    }

    private fun shortLoggerName(loggerName: String?): String = loggerName?.substringAfterLast('.') ?: "unknown"
}
