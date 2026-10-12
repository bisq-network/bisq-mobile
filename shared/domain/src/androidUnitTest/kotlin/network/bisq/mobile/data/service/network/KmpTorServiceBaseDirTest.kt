package network.bisq.mobile.data.service.network

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * On the node the base dir comes from the bisq2 application service, which is expensive to build.
 * The Tor service must not ask for it until it needs its files.
 */
class KmpTorServiceBaseDirTest {
    @Test
    fun `the base dir is not resolved at construction and is resolved once when Tor files are needed`() =
        runTest {
            val root = Files.createTempDirectory("kmpTorBaseDir").toFile().apply { deleteOnExit() }
            val cookie = File(File(root, "tor").apply { mkdirs() }, "control_auth_cookie").apply { writeText("stale") }
            var resolutions = 0
            val service =
                KmpTorService(
                    baseDirProvider = {
                        resolutions++
                        root.absolutePath.toPath()
                    },
                )

            assertEquals(0, resolutions)

            service.cleanStaleControlState()
            service.cleanStaleControlState()

            assertFalse(cookie.exists())
            assertEquals(1, resolutions)
        }
}
