package network.bisq.mobile.presentation.common.share

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLogFileProviderTest {
    @Test
    fun `the no-op provider offers no log file`() =
        runTest {
            assertNull(NoLogFileProvider.logFile())
        }

    @Test
    fun `a log file carries its path and name`() {
        val logFile = AppLogFile(path = "/data/files/bisq.log", name = "bisq.log")

        assertEquals("/data/files/bisq.log", logFile.path)
        assertEquals("bisq.log", logFile.name)
    }
}
