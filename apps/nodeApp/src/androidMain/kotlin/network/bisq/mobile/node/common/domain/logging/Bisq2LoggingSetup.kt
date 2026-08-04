package network.bisq.mobile.node.common.domain.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import network.bisq.mobile.node.BuildConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Programmatic logback configuration for the bisq2 core jars embedded in the node app (issue #767).
 *
 * The core logs via slf4j -> logback-classic and auto-configures from the logback.xml BUNDLED IN
 * the bisq:common jar: a ConsoleAppender (ANSI-colored) whose output the app's SystemOutFilter
 * swallowed, so core logs never reached logcat. We reset that configuration and attach
 * [Bisq2LogcatAppender] instead - programmatically rather than by shadowing the jar's logback.xml
 * with our own, because classpath resource ordering across jars is undefined on Android while this
 * is deterministic (and keeps levels runtime-mutable for a future dev-settings toggle).
 *
 * Must run BEFORE the first bisq2 service starts logging (see NodeMainApplication.onCreated).
 */
object Bisq2LoggingSetup {
    /**
     * ============================ TWEAK ME (debug builds) =============================
     * The bisq2 core is very verbose by default (the jar's logback.xml leaves root at DEBUG).
     * [DEBUG_ROOT_LEVEL] is the baseline - set it WITHOUT touching code via
     * `BISQ2_LOG_LEVEL=DEBUG` in local.properties (default lives in gradle.properties as
     * `node.bisq2.log.level`; unparseable values fall back to INFO).
     * [DEBUG_LOGGER_LEVELS] raises/lowers specific subtrees. Chasing something specific?
     * Set its package to DEBUG/TRACE here - entries win over root.
     * ==================================================================================
     */
    val DEBUG_ROOT_LEVEL: Level = Level.toLevel(BuildConfig.BISQ2_LOG_LEVEL, Level.INFO)
    val DEBUG_LOGGER_LEVELS: Map<String, Level> =
        mapOf(
            // Per-connection / inventory-request chatter floods logcat at INFO during bootstrap.
            "bisq.network" to Level.WARN,
            // Tor bootstrap progress is useful; kmp-tor noise is already handled app-side.
            "bisq.network.tor" to Level.INFO,
        )

    /**
     * Reconfigures logback for the embedded bisq2 core.
     *
     * Debug builds: core logs flow to logcat under the fixed `Bisq2` tag ([Bisq2LogcatAppender])
     * with the levels above. Release builds: the core stays fully silent (root OFF) - core logs can
     * carry peer/onion addresses, and release System.out is already hard-blocked; this keeps the
     * privacy posture identical on the logback route.
     *
     * @return true if logback was found and reconfigured, false when the slf4j binding is not
     *         logback (nothing to do - e.g. unit-test environments with a different binding).
     */
    fun setup(isDebugBuild: Boolean): Boolean {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return false
        // Drop the jar-bundled configuration (ANSI ConsoleAppender writing into the stdout filter).
        loggerContext.reset()

        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        if (!isDebugBuild) {
            rootLogger.level = Level.OFF
            return true
        }

        val appender =
            Bisq2LogcatAppender().apply {
                context = loggerContext
                start()
            }
        rootLogger.level = DEBUG_ROOT_LEVEL
        rootLogger.addAppender(appender)
        DEBUG_LOGGER_LEVELS.forEach { (loggerName, level) ->
            loggerContext.getLogger(loggerName).level = level
        }
        return true
    }
}
