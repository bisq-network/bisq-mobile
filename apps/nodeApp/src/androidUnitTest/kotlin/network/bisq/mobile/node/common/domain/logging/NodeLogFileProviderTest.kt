package network.bisq.mobile.node.common.domain.logging

import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [NodeLogFileProvider]: the bisq2 log file is only offered when it actually exists and has
 * content, and what is handed out for sharing is a redacted, capped copy while the raw file stays as is.
 */
class NodeLogFileProviderTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val onion = "ygcd52prbkt5al4yscyj5oythgz65pdbzq2p36nrisxdhzcruueaxwid.onion"
    private val tradeId = "4c46d7a1-9f3e-4b7a-8c21-0d5e6f7a8b9c"

    private fun provider() = NodeLogFileProvider(tempFolder.root, exportDir())

    private fun exportDir(): File = File(tempFolder.root, "cache").apply { mkdirs() }

    private fun sharedCopy(): File = File(File(exportDir(), NodeLogFileProvider.EXPORT_DIR_NAME), NodeLogFileProvider.SHARED_LOG_FILE_NAME)

    @Test
    fun `preparing for sharing fails without a log file`() =
        runTest {
            assertTrue(provider().prepareForSharing().isFailure)
        }

    @Test
    fun `preparing for sharing writes a redacted copy and leaves the raw log untouched`() =
        runTest {
            val raw = "Sept-14 12:23:57.883 [main] INFO  b.t.TradeService: Trade $tradeId with $onion:37802 confirmed\n"
            val rawFile = tempFolder.newFile("bisq.log").apply { writeText(raw) }

            val shared = provider().prepareForSharing().getOrThrow()

            assertEquals(NodeLogFileProvider.SHARED_LOG_FILE_NAME, shared.name)
            assertEquals(sharedCopy().absolutePath, shared.path)
            val lines = sharedCopy().readLines()
            assertTrue(lines[0].startsWith("# bisq.log, redacted for sharing"), lines[0])
            assertTrue(lines[1].contains("The whole file is included"), lines[1])
            assertEquals("Sept-14 12:23:57.883 [main] INFO  b.t.TradeService: Trade <uuid#1> with <onion#1>:37802 confirmed", lines[2])
            assertTrue(lines.last().startsWith("# redacted: 1 onion, 1 uuid"), lines.last())
            assertFalse(sharedCopy().readText().contains(onion))
            assertEquals(raw, rawFile.readText())
        }

    @Test
    fun `only the tail of a large log is shared starting at a complete line`() =
        runTest {
            val line = "Sept-14 12:23:57.883 [main] INFO  b.n.Connection: peer $onion:37802 keep-alive"
            val rawFile = tempFolder.newFile("bisq.log")
            rawFile.bufferedWriter().use { out ->
                var written = 0L
                var n = 0
                while (written < NodeLogFileProvider.SHARED_LOG_TAIL_BYTES + 200_000) {
                    val l = "$line #${n++}"
                    out.write(l)
                    out.newLine()
                    written += l.length + 1
                }
            }

            provider().prepareForSharing().getOrThrow()

            val lines = sharedCopy().readLines()
            assertTrue(lines[1].contains("Only the most recent 2 MB"), lines[1])
            // The first content line is complete: it starts with the timestamp, not a cut-off fragment.
            assertTrue(lines[2].startsWith("Sept-14 12:23:57.883 [main] INFO"), lines[2])
            assertTrue(Regex(".* #\\d+$").matches(lines[2]), lines[2])
            assertFalse(lines[2].contains("#0"), "the beginning of the file was dropped: " + lines[2])
            assertTrue(sharedCopy().length() <= NodeLogFileProvider.SHARED_LOG_TAIL_BYTES + 1_000)
            assertFalse(sharedCopy().readText().contains(onion))
        }

    @Test
    fun `a previous redacted copy is replaced`() =
        runTest {
            tempFolder.newFile("bisq.log").writeText("first run $onion\n")
            provider().prepareForSharing().getOrThrow()
            File(tempFolder.root, "bisq.log").writeText("second run\n")

            provider().prepareForSharing().getOrThrow()

            val text = sharedCopy().readText()
            assertTrue(text.contains("second run"), text)
            assertFalse(text.contains("first run"), text)
        }

    @Test
    fun `no log file means nothing to share`() =
        runTest {
            assertNull(provider().logFile())
        }

    @Test
    fun `an empty log file is treated as missing`() =
        runTest {
            tempFolder.newFile("bisq.log")

            assertNull(provider().logFile())
        }

    @Test
    fun `an unreadable log file is not offered`() =
        runTest {
            val file = tempFolder.newFile("bisq.log").apply { writeText("line\n") }
            assumeTrue("Cannot drop read permission as this user", file.setReadable(false, false))

            assertNull(provider().logFile())
        }

    @Test
    fun `an existing log file is offered by name and path`() =
        runTest {
            val file = tempFolder.newFile("bisq.log").apply { writeText("first line\nsecond line\n") }

            val logFile = provider().logFile()

            assertEquals("bisq.log", logFile?.name)
            assertEquals(file.absolutePath, logFile?.path)
        }
}
