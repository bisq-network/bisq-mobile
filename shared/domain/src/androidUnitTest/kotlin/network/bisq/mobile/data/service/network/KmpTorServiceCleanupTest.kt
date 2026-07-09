package network.bisq.mobile.data.service.network

import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [KmpTorService.cleanStaleControlState] removes only the ephemeral control-plane files
 * (which cause the "515 Authentication cookie did not match expected value" hang when stale) and
 * leaves the persistent Tor data — notably the onion service identity keys — untouched.
 */
class KmpTorServiceCleanupTest {
    private fun createTorDir(): Pair<KmpTorService, File> {
        val root = Files.createTempDirectory("kmpTorCleanup").toFile().apply { deleteOnExit() }
        val torDir = File(root, "tor").apply { mkdirs() }
        val service = KmpTorService(root.absolutePath.toPath())
        return service to torDir
    }

    @Test
    fun `cleanStaleControlState removes stale control files`() =
        runTest {
            val (service, torDir) = createTorDir()
            val cookie = File(torDir, "control_auth_cookie").apply { writeText("stale-cookie") }
            val controlPort = File(torDir, "control-port.txt").apply { writeText("PORT=127.0.0.1:9051") }
            val controlPortBackup = File(torDir, "control-port-backup.txt").apply { writeText("PORT=127.0.0.1:9051") }

            service.cleanStaleControlState()

            assertFalse(cookie.exists(), "control auth cookie should be deleted")
            assertFalse(controlPort.exists(), "control-port file should be deleted")
            assertFalse(controlPortBackup.exists(), "control-port backup file should be deleted")
        }

    @Test
    fun `cleanStaleControlState preserves onion identity and other data dir contents`() =
        runTest {
            val (service, torDir) = createTorDir()
            File(torDir, "control_auth_cookie").writeText("stale-cookie")
            val keysDir = File(torDir, "keys").apply { mkdirs() }
            val onionKey = File(keysDir, "hs_ed25519_secret_key").apply { writeText("onion-identity") }
            val cachedDescriptors = File(torDir, "cached-microdescs").apply { writeText("descriptors") }

            service.cleanStaleControlState()

            assertTrue(onionKey.exists(), "onion identity key must survive cleanup")
            assertTrue(cachedDescriptors.exists(), "unrelated data dir files must survive cleanup")
        }

    @Test
    fun `cleanStaleControlState is a no-op when nothing to clean`() =
        runTest {
            val (service, _) = createTorDir()
            // No control files present — must not throw.
            service.cleanStaleControlState()
        }
}
