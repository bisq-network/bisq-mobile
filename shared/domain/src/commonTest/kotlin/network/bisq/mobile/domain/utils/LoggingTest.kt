package network.bisq.mobile.domain.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class LoggingTest {
    private class NamedLogging : Logging

    @Test
    fun `named class logs with its simple name as tag`() {
        assertEquals("NamedLogging", NamedLogging().log.tag)
    }

    @Test
    fun `anonymous object logs with the default tag`() {
        val anonymous = object : Logging {}
        assertEquals(DEFAULT_LOGGER_TAG, anonymous.log.tag)
    }

    @Test
    fun `explicit string tag is kept as given`() {
        assertEquals("ResourceBundle", getLogger("ResourceBundle").tag)
        assertEquals("", getLogger("").tag)
    }

    @Test
    fun `release logger logs errors only through the redacting writer`() {
        val logger = createLogger("Release", isDebug = false)

        assertEquals("Release", logger.tag)
        assertEquals(Severity.Error, logger.config.minSeverity)
        assertEquals(1, logger.config.logWriterList.size)
        assertIs<RedactingLogWriter>(logger.config.logWriterList.single())
    }

    @Test
    fun `release loggers share one config`() {
        assertSame(
            createLogger("First", isDebug = false).config,
            createLogger("Second", isDebug = false).config,
        )
    }

    @Test
    fun `debug logger uses the global kermit config`() {
        val logger = createLogger("Debug", isDebug = true)

        assertEquals("Debug", logger.tag)
        assertSame(Logger.config, logger.config)
    }

    // A pass shows no crash under contention; it cannot prove the absence of a race
    @Test
    fun `concurrent first use of many tags does not crash`() =
        runTest {
            val workers = 8
            val tagsPerWorker = 2_000
            val tags =
                withContext(Dispatchers.Default) {
                    (0 until workers)
                        .map { worker ->
                            async {
                                (0 until tagsPerWorker).map { i -> getLogger("stress-$worker-$i").tag }
                            }
                        }.awaitAll()
                        .flatten()
                }

            assertEquals(workers * tagsPerWorker, tags.toSet().size)
        }
}
