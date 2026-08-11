package network.bisq.mobile.data.service.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test

/**
 * Real-socket tests for the bootstrap control-port probe. [KmpTorService] construction is
 * inert (lazy DI injection, plain state flows), so no Koin setup is needed. The probe's
 * initial 500ms grace delay runs in real time on Dispatchers.IO — each test takes ~1s.
 */
class KmpTorServiceControlPortProbeTest {
    private fun service() = KmpTorService(baseDir = "build/tmp/kmp-tor-test".toPath())

    @Test
    fun `returns once the control port accepts a connection`() =
        runTest {
            val selector = SelectorManager(Dispatchers.IO)
            val server = aSocket(selector).tcp().bind("127.0.0.1", 0)
            try {
                val port = (server.localAddress as InetSocketAddress).port
                service().verifyControlPortAccessible(port)
            } finally {
                server.close()
                selector.close()
            }
        }

    @Test
    fun `gives up without throwing after retries when nothing listens on the port`() =
        runTest {
            // Bind then release to obtain a local port that is almost certainly closed.
            val selector = SelectorManager(Dispatchers.IO)
            val probe = aSocket(selector).tcp().bind("127.0.0.1", 0)
            val port = (probe.localAddress as InetSocketAddress).port
            probe.close()
            try {
                // Bootstrap continues even when the probe fails — this must not throw.
                service().verifyControlPortAccessible(port)
            } finally {
                selector.close()
            }
        }
}
