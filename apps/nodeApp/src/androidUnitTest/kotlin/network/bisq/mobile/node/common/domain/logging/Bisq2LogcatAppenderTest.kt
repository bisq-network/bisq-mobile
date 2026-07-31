package network.bisq.mobile.node.common.domain.logging

import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import network.bisq.mobile.node.common.test_utils.TestApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [Bisq2LogcatAppender]: logback level -> logcat priority mapping, the fixed `Bisq2` tag with
 * the short logger name folded into the message, and throwable stack traces reaching logcat.
 */
@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class Bisq2LogcatAppenderTest {
    private val loggerContext = LoggerContext()
    private lateinit var appender: Bisq2LogcatAppender

    @Before
    fun setUp() {
        ShadowLog.clear()
        appender =
            Bisq2LogcatAppender().apply {
                context = loggerContext
                start()
            }
    }

    private fun event(
        level: Level,
        message: String,
        loggerName: String = "bisq.network.p2p.node.Node",
        throwable: Throwable? = null,
    ): LoggingEvent =
        LoggingEvent(
            Bisq2LogcatAppenderTest::class.java.name,
            loggerContext.getLogger(loggerName),
            level,
            message,
            throwable,
            null,
        )

    private fun singleLogged(): ShadowLog.LogItem {
        val items = ShadowLog.getLogsForTag(Bisq2LogcatAppender.TAG)
        assertEquals(1, items.size, "exactly one logcat entry expected under the Bisq2 tag")
        return items.first()
    }

    @Test
    fun `error events map to logcat ERROR priority`() {
        appender.doAppend(event(Level.ERROR, "boom"))

        assertEquals(Log.ERROR, singleLogged().type)
    }

    @Test
    fun `warn events map to logcat WARN priority`() {
        appender.doAppend(event(Level.WARN, "careful"))

        assertEquals(Log.WARN, singleLogged().type)
    }

    @Test
    fun `info events map to logcat INFO priority`() {
        appender.doAppend(event(Level.INFO, "fyi"))

        assertEquals(Log.INFO, singleLogged().type)
    }

    @Test
    fun `debug events map to logcat DEBUG priority`() {
        appender.doAppend(event(Level.DEBUG, "details"))

        assertEquals(Log.DEBUG, singleLogged().type)
    }

    @Test
    fun `trace events map to logcat VERBOSE priority`() {
        appender.doAppend(event(Level.TRACE, "very detailed"))

        assertEquals(Log.VERBOSE, singleLogged().type)
    }

    @Test
    fun `message carries the short logger name under the fixed Bisq2 tag`() {
        appender.doAppend(event(Level.INFO, "Bootstrap complete", loggerName = "bisq.network.NetworkService"))

        val item = singleLogged()
        assertEquals(Bisq2LogcatAppender.TAG, item.tag)
        assertEquals("[NetworkService] Bootstrap complete", item.msg)
    }

    @Test
    fun `throwable stack trace is appended to the message`() {
        appender.doAppend(event(Level.ERROR, "it broke", throwable = IllegalStateException("root cause here")))

        val msg = singleLogged().msg
        assertTrue(msg.contains("it broke"))
        assertTrue(msg.contains("IllegalStateException"), "throwable type must be included")
        assertTrue(msg.contains("root cause here"), "throwable message must be included")
    }
}
