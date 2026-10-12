package network.bisq.mobile.node.common.domain.analytics

import bisq.common.network.TransportType
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The analytics bootstrap polls for the SOCKS port from the main thread while the bisq2 application
 * service is still being built in the background. Polling must wait for it, not build it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Bisq2SocksPortProviderTest : NodeKoinIntegrationTestBase() {
    private companion object {
        const val SOCKS_PORT = 9050
    }

    @Test
    fun `polling does not build the application service and resolves once it is available`() =
        runTest {
            var builds = 0
            val provider =
                AndroidApplicationService.Provider {
                    builds++
                    mockk(relaxed = true)
                }
            val socksPortProvider = Bisq2SocksPortProvider(provider)

            val port = async { socksPortProvider.awaitSocksPort() }
            advanceTimeBy(10_000)
            runCurrent()

            assertEquals(0, builds)
            assertFalse(port.isCompleted)

            provider.applicationService = applicationServiceWithSocksPort()
            advanceTimeBy(10_000)
            runCurrent()

            assertEquals(SOCKS_PORT, port.await())
            assertEquals(0, builds)
        }

    private fun applicationServiceWithSocksPort(): AndroidApplicationService {
        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every {
            applicationService.networkService.serviceNodesByTransport
                .getSocksProxy(TransportType.TOR)
        } returns Optional.of(Socks5Proxy("127.0.0.1", SOCKS_PORT))
        return applicationService
    }
}
