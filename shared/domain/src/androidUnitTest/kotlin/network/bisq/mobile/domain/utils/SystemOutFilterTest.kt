package network.bisq.mobile.domain.utils

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [SystemOutFilter]'s two intake routes: the print/println overrides and - the issue #767
 * regression - the raw OutputStream write() route that logback's ConsoleAppender (bundled in the
 * bisq2 jars' logback.xml) uses exclusively. Before the fix, write() bytes drained into the
 * NullOutputStream the class extends, silently discarding every bisq2 core log line in debug AND
 * release builds.
 */
class SystemOutFilterTest {
    private val captured = ByteArrayOutputStream()
    private val originalStream = PrintStream(captured, true)

    private fun debugFilter() = SystemOutFilter(originalStream, isDebugBuild = true, tag = "TestTag")

    private fun releaseFilter() = SystemOutFilter(originalStream, isDebugBuild = false, tag = "TestTag")

    private fun output(): String = captured.toString()

    // ------------------------------- raw write() route (issue #767) -------------------------------

    @Test
    fun `bytes written via write are emitted instead of being discarded`() {
        val filter = debugFilter()
        val line = "2026-01-01 INFO bisq.network.NetworkService: Bootstrapping\n"

        filter.write(line.toByteArray(), 0, line.toByteArray().size)

        assertTrue(output().contains("Bootstrapping"), "write() bytes must reach the original stream, not the null sink")
    }

    @Test
    fun `chunked writes are assembled into one line before filtering`() {
        val filter = debugFilter()
        val part1 = "half a ".toByteArray()
        val part2 = "log line\n".toByteArray()

        filter.write(part1, 0, part1.size)
        assertFalse(output().contains("half a"), "no emit before the line is complete")
        filter.write(part2, 0, part2.size)

        assertTrue(output().contains("half a log line"))
    }

    @Test
    fun `single byte writes accumulate into a line`() {
        val filter = debugFilter()
        "ok\n".forEach { filter.write(it.code) }

        assertEquals("ok", output().trim())
    }

    @Test
    fun `filtered pattern via write is prefixed in debug builds`() {
        val filter = debugFilter()
        val line = "marketPriceByCurrencyMap={...verbose...}\n"

        filter.write(line.toByteArray(), 0, line.toByteArray().size)

        assertTrue(output().contains("[TestTag][FILTERED]"), "debug builds keep filtered content visible, prefixed")
        assertTrue(output().contains("marketPriceByCurrencyMap"))
    }

    @Test
    fun `filtered pattern via write is dropped when not a debug build`() {
        val filter = releaseFilter()
        val line = "marketPriceByCurrencyMap={...verbose...}\n"

        filter.write(line.toByteArray(), 0, line.toByteArray().size)

        assertEquals("", output(), "smart-filtering mode outside debug drops filtered lines entirely")
    }

    @Test
    fun `non-filtered line via write passes through when not a debug build`() {
        val filter = releaseFilter()
        val line = "Important core warning\n"

        filter.write(line.toByteArray(), 0, line.toByteArray().size)

        assertTrue(output().contains("Important core warning"))
    }

    @Test
    fun `crlf line endings do not leak carriage returns into the emitted line`() {
        val filter = debugFilter()
        val line = "windows style\r\n".toByteArray()

        filter.write(line, 0, line.size)

        assertTrue(output().contains("windows style"))
        assertFalse(output().contains("\r"), "the CR must be stripped entirely")
    }

    @Test
    fun `multi-byte character split across writes is not corrupted`() {
        val filter = debugFilter()
        val bytes = "café line\n".toByteArray(Charsets.UTF_8)
        // 'é' is two bytes in UTF-8 (indices 3..4); split the write between them.
        filter.write(bytes, 0, 4)
        filter.write(bytes, 4, bytes.size - 4)

        assertTrue(output().contains("café line"), "a split multi-byte char must decode intact")
    }

    @Test
    fun `flush emits a buffered partial line`() {
        val filter = debugFilter()
        val partial = "no newline yet".toByteArray()

        filter.write(partial, 0, partial.size)
        assertFalse(output().contains("no newline yet"))
        filter.flush()

        assertTrue(output().contains("no newline yet"))
    }

    @Test
    fun `multiple lines in one write are each filtered independently`() {
        val filter = debugFilter()
        val lines = "clean line\nMarketPrice{noisy}\nanother clean\n".toByteArray()

        filter.write(lines, 0, lines.size)

        val out = output()
        assertTrue(out.contains("clean line"))
        assertTrue(out.contains("another clean"))
        assertTrue(out.contains("[TestTag][FILTERED] MarketPrice{noisy}"))
    }

    // ------------------------------- print/println route (existing behavior pin) ------------------

    @Test
    fun `println passes non-filtered content through`() {
        val filter = debugFilter()
        filter.println("hello from println")

        assertTrue(output().contains("hello from println"))
    }

    @Test
    fun `println prefixes filtered content in debug builds`() {
        val filter = debugFilter()
        filter.println("PriceQuote(1234)")

        assertTrue(output().contains("[TestTag][FILTERED] PriceQuote(1234)"))
    }

    @Test
    fun `println drops filtered content when not a debug build`() {
        val filter = releaseFilter()
        filter.println("PriceQuote(1234)")

        assertEquals("", output())
    }
}
