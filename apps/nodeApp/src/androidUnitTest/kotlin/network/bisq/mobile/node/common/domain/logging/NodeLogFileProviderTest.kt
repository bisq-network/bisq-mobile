package network.bisq.mobile.node.common.domain.logging

import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers [NodeLogFileProvider]: the bisq2 log file is only offered when it actually exists and has
 * content, and it is offered by path so it can be shared without being read into memory.
 */
class NodeLogFileProviderTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `no log file means nothing to share`() =
        runTest {
            assertNull(NodeLogFileProvider(tempFolder.root).logFile())
        }

    @Test
    fun `an empty log file is treated as missing`() =
        runTest {
            tempFolder.newFile("bisq.log")

            assertNull(NodeLogFileProvider(tempFolder.root).logFile())
        }

    @Test
    fun `an unreadable log file is not offered`() =
        runTest {
            val file = tempFolder.newFile("bisq.log").apply { writeText("line\n") }
            assumeTrue("Cannot drop read permission as this user", file.setReadable(false, false))

            assertNull(NodeLogFileProvider(tempFolder.root).logFile())
        }

    @Test
    fun `an existing log file is offered by name and path`() =
        runTest {
            val file = tempFolder.newFile("bisq.log").apply { writeText("first line\nsecond line\n") }

            val logFile = NodeLogFileProvider(tempFolder.root).logFile()

            assertEquals("bisq.log", logFile?.name)
            assertEquals(file.absolutePath, logFile?.path)
        }
}
