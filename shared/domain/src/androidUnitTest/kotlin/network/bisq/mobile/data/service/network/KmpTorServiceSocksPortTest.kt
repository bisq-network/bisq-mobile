package network.bisq.mobile.data.service.network

import okio.Path.Companion.toPath
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The analytics SDK keeps the SOCKS port it was initialised with, so the port Tor bound is remembered
 * and asked for again when Tor restarts.
 */
class KmpTorServiceSocksPortTest {
    private val service =
        KmpTorService(
            Files
                .createTempDirectory("kmpTorSocksPort")
                .toFile()
                .apply { deleteOnExit() }
                .absolutePath
                .toPath(),
        )

    @Test
    fun `the bound socks port is remembered for the next start`() {
        service.tryParseSockPort("Socks listener listening on port 54934.")

        assertEquals(54934, service.lastSocksPort)
        assertEquals(54934, service.socksPort.value)
    }

    @Test
    fun `a reassigned port replaces the remembered one`() {
        service.tryParseSockPort("Socks listener listening on port 54934.")

        service.tryParseSockPort("Socks listener listening on port 54970.")

        assertEquals(54970, service.lastSocksPort)
    }

    @Test
    fun `other notices leave the remembered port alone`() {
        service.tryParseSockPort("Bootstrapped 100% (done): Done")

        assertNull(service.lastSocksPort)
    }
}
