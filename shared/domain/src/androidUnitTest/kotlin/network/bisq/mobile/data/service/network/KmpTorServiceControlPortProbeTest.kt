package network.bisq.mobile.data.service.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
            SelectorManager(Dispatchers.IO).use { selector ->
                aSocket(selector).tcp().bind("127.0.0.1", 0).use { server ->
                    val port = (server.localAddress as InetSocketAddress).port
                    service().verifyControlPortAccessible(port)
                }
            }
        }

    @Test
    fun `gives up without throwing after retries when nothing listens on the port`() =
        runTest {
            SelectorManager(Dispatchers.IO).use { selector ->
                // Bind then release to obtain a local port that is almost certainly closed.
                val port =
                    aSocket(selector).tcp().bind("127.0.0.1", 0).use { probe ->
                        (probe.localAddress as InetSocketAddress).port
                    }
                // Bootstrap continues even when the probe fails — this must not throw.
                service().verifyControlPortAccessible(port)
            }
        }

    @Test
    fun `propagates cancellation to the caller instead of swallowing it`() {
        // runBlocking, not runTest: the probe's delays run in real time on Dispatchers.IO, so we
        // need a real clock to cancel mid-flight. Guards the fix that rethrows CancellationException
        // ahead of the broad catch — a swallowed cancellation would make this hang or complete.
        runBlocking {
            SelectorManager(Dispatchers.IO).use { selector ->
                val port =
                    aSocket(selector).tcp().bind("127.0.0.1", 0).use { probe ->
                        (probe.localAddress as InetSocketAddress).port
                    }
                val deferred = async(Dispatchers.IO) { service().verifyControlPortAccessible(port) }
                delay(100) // well inside the probe's 500ms grace delay
                deferred.cancel()
                assertFailsWith<CancellationException> { deferred.await() }
            }
        }
    }
}
