package network.bisq.mobile.node.service.tor

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import network.bisq.mobile.domain.service.network.KmpTorService

/**
 * This test asserts the restart path's only filesystem mutation (Tor stop+purge)
 * does not touch Bisq2's db/private or db/settings directories.
 *
 * We exercise KmpTorService.stopAndPurgeWorkingDir against a temporary layout:
 *   <tmp>/Bisq2_mobile/
 *     ├── tor/           (should be deleted)
 *     └── db/
 *         ├── private/   (must remain)
 *         └── settings/  (must remain)
 */
class RestartAppSafetyTest {

    @Test
    fun stopAndPurgeTor_doesNotDeleteDbPrivateOrSettings() = runBlocking {
        val root = Files.createTempDirectory("restartAppSafety").toFile().apply { deleteOnExit() }
        val baseDir = File(root, "Bisq2_mobile").apply { mkdirs() }
        val torDir = File(baseDir, "tor").apply { mkdirs() }
        val torCache = File(baseDir, "tor/cache").apply { mkdirs() }
        val controlPort = File(baseDir, "tor/control-port.txt").apply { writeText("PORT=127.0.0.1:12345\n") }
        val controlPortBackup = File(baseDir, "tor/control-port-backup.txt").apply { writeText("PORT=127.0.0.1:12345\n") }
        File(torCache, "dummy").apply { writeText("x") }

        val dbDir = File(baseDir, "db").apply { mkdirs() }
        val privateDir = File(dbDir, "private").apply { mkdirs() }
        val settingsDir = File(dbDir, "settings").apply { mkdirs() }
        val privateMarker = File(privateDir, "profile.bin").apply { writeText("data") }
        val settingsMarker = File(settingsDir, "settings.json").apply { writeText("{}") }

        val service = KmpTorService(baseDir.absolutePath.toPath())

        // Act: this is what restartApp() calls after shutting down services.
        service.stopAndPurgeWorkingDir(timeoutMs = 50)

        // Assert: tor dir purged
        assertFalse(torDir.exists(), "tor directory must be purged by stopAndPurgeWorkingDir")
        // Assert: db/private and db/settings intact
        assertTrue(privateDir.exists() && privateDir.isDirectory, "db/private must remain intact")
        assertTrue(settingsDir.exists() && settingsDir.isDirectory, "db/settings must remain intact")
        assertTrue(privateMarker.exists(), "db/private content must remain")
        assertTrue(settingsMarker.exists(), "db/settings content must remain")
    }
}

