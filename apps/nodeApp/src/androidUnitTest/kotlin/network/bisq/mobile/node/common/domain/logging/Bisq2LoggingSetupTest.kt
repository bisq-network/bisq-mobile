package network.bisq.mobile.node.common.domain.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import network.bisq.mobile.node.common.test_utils.TestApplication
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [Bisq2LoggingSetup] against the REAL slf4j binding the node app ships (logback-classic,
 * pulled transitively by the bisq2 jars): debug builds get the logcat appender + tuned levels,
 * release builds silence the core entirely, and an actual slf4j log call lands in logcat end to end.
 */
@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class Bisq2LoggingSetupTest {
    private val loggerContext get() = LoggerFactory.getILoggerFactory() as LoggerContext

    @After
    fun tearDown() {
        // Leave no configuration behind for other tests sharing the slf4j static state.
        loggerContext.reset()
    }

    @Test
    fun `debug setup attaches the logcat appender with the tuned levels`() {
        assertTrue(Bisq2LoggingSetup.setup(isDebugBuild = true), "logback must be the resolved binding in node tests")

        val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        assertEquals(Bisq2LoggingSetup.DEBUG_ROOT_LEVEL, root.level)
        assertTrue(
            root.iteratorForAppenders().asSequence().any { it is Bisq2LogcatAppender },
            "the logcat appender must be attached to the root logger",
        )
        Bisq2LoggingSetup.DEBUG_LOGGER_LEVELS.forEach { (loggerName, level) ->
            assertEquals(level, loggerContext.getLogger(loggerName).level, "tuned level for $loggerName")
        }
    }

    @Test
    fun `release setup silences the core and attaches nothing`() {
        assertTrue(Bisq2LoggingSetup.setup(isDebugBuild = false))

        val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        assertEquals(Level.OFF, root.level)
        assertFalse(root.iteratorForAppenders().hasNext(), "release builds must not route core logs anywhere")
    }

    @Test
    fun `after debug setup a real slf4j log call reaches logcat`() {
        ShadowLog.clear()
        Bisq2LoggingSetup.setup(isDebugBuild = true)

        LoggerFactory.getLogger("bisq.some.core.Component").info("core says hi")

        val items = ShadowLog.getLogsForTag(Bisq2LogcatAppender.TAG)
        assertEquals(1, items.size)
        assertEquals("[Component] core says hi", items.first().msg)
    }

    @Test
    fun `after debug setup a suppressed-package log call below its tuned level does not reach logcat`() {
        ShadowLog.clear()
        Bisq2LoggingSetup.setup(isDebugBuild = true)

        // bisq.network is tuned to WARN precisely because its INFO chatter floods logcat.
        LoggerFactory.getLogger("bisq.network.SomeChattyService").info("per-connection noise")
        LoggerFactory.getLogger("bisq.network.SomeChattyService").warn("actual problem")

        val messages = ShadowLog.getLogsForTag(Bisq2LogcatAppender.TAG).map { it.msg }
        assertEquals(listOf("[SomeChattyService] actual problem"), messages)
    }

    @Test
    fun `after release setup a real slf4j log call is fully silenced`() {
        ShadowLog.clear()
        Bisq2LoggingSetup.setup(isDebugBuild = false)

        LoggerFactory.getLogger("bisq.some.core.Component").error("even errors stay out of logcat")

        assertTrue(ShadowLog.getLogsForTag(Bisq2LogcatAppender.TAG).isEmpty())
    }
}
