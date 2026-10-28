package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Guards the iOS Tor DNS-leak contract of [createHttpClient].
 *
 * Android blocks local resolution with `NoDns`. NSURLSession has no resolver hook, so iOS relies
 * on CFNetwork's SOCKS5 client sending the hostname to the proxy (ATYP 0x03) instead of resolving
 * it locally and sending an IP (ATYP 0x01 / 0x04). A fake SOCKS5 proxy records the CONNECT
 * request and refuses it, so no traffic leaves the machine when the contract holds.
 *
 * This proves the hostname is handed to the proxy. It cannot prove that no parallel system DNS
 * lookup happens; that needs a packet capture on a device.
 *
 * Runs on real sockets and a real clock (runBlocking), so each case takes well under a second.
 */
class TorSocksDnsLeakIosTest {
    @Test
    fun httpToClearnetHostSendsHostnameToProxy() = assertHostnameSentToProxy(url = "http://example.com:8090/api/v1/settings", expectedHost = "example.com", expectedPort = 8090)

    @Test
    fun httpsWithTlsFingerprintToClearnetHostSendsHostnameToProxy() =
        assertHostnameSentToProxy(
            url = "https://example.com/api/v1/settings",
            expectedHost = "example.com",
            expectedPort = 443,
            tlsFingerprint = DUMMY_TLS_FINGERPRINT,
        )

    @Test
    fun httpToOnionHostSendsHostnameToProxy() = assertHostnameSentToProxy(url = "http://$ONION_HOST:8090/api/v1/settings", expectedHost = ONION_HOST, expectedPort = 8090)

    @Test
    fun websocketToClearnetHostSendsHostnameToProxy() = assertHostnameSentToProxy(url = "ws://example.com:8090/websocket", expectedHost = "example.com", expectedPort = 8090, websocket = true)

    @Test
    fun websocketToOnionHostSendsHostnameToProxy() = assertHostnameSentToProxy(url = "ws://$ONION_HOST:8090/websocket", expectedHost = ONION_HOST, expectedPort = 8090, websocket = true)

    private fun assertHostnameSentToProxy(
        url: String,
        expectedHost: String,
        expectedPort: Int,
        tlsFingerprint: String? = null,
        websocket: Boolean = false,
    ) = runBlocking {
        withFakeSocksProxy { proxyPort, connectRequests ->
            val proxyConfig =
                HttpClientSettings(
                    bisqApiUrl = null,
                    tlsFingerprint = tlsFingerprint,
                    selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
                    externalProxyUrl = "127.0.0.1:$proxyPort",
                    isTorProxy = true,
                ).bisqProxyConfig()
            assertNotNull(proxyConfig)

            val client = createHttpClient(expectedHost, tlsFingerprint, proxyConfig)
            // The proxy refuses every CONNECT, so the request itself always fails; only what
            // reached the proxy matters.
            val requestJob =
                launch {
                    ignoringFailures {
                        if (websocket) client.webSocket(url) {} else client.get(url)
                    }
                }
            try {
                val request = withTimeoutOrNull(REQUEST_TIMEOUT_MS) { connectRequests.receive() }
                assertNotNull(request, "No SOCKS CONNECT reached the proxy for '$url': the client bypassed it")
                assertEquals(
                    ATYP_DOMAIN_NAME,
                    request.addressType,
                    "Client resolved '$expectedHost' locally and sent ${request.host} to the proxy (DNS leak)",
                )
                assertEquals(expectedHost, request.host)
                assertEquals(expectedPort, request.port)
            } finally {
                requestJob.cancel()
                client.close()
            }
        }
    }

    private suspend fun withFakeSocksProxy(block: suspend CoroutineScope.(port: Int, connectRequests: Channel<SocksConnectRequest>) -> Unit) {
        SelectorManager(Dispatchers.Default).use { selector ->
            aSocket(selector).tcp().bind("127.0.0.1", 0).use { server ->
                val port = (server.localAddress as InetSocketAddress).port
                val connectRequests = Channel<SocksConnectRequest>(Channel.UNLIMITED)
                coroutineScope {
                    val acceptJob =
                        launch {
                            while (isActive) {
                                val socket = server.accept()
                                launch { socket.use { ignoringFailures { handleSocksHandshake(it, connectRequests) } } }
                            }
                        }
                    try {
                        block(port, connectRequests)
                    } finally {
                        acceptJob.cancel()
                    }
                }
            }
        }
    }

    /** Failures are expected here (refused CONNECT, closed sockets); cancellation must still propagate. */
    private inline fun ignoringFailures(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /** Minimal SOCKS5 server side (RFC 1928): accept "no auth", record CONNECT, refuse it. */
    private suspend fun handleSocksHandshake(
        socket: Socket,
        connectRequests: Channel<SocksConnectRequest>,
    ) {
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        check(input.readByte() == SOCKS_VERSION) { "Not a SOCKS5 greeting" }
        val methodCount = input.readByte().toInt() and 0xFF
        input.readByteArray(methodCount)
        output.writeByteArray(byteArrayOf(SOCKS_VERSION, METHOD_NO_AUTH))

        input.readByte() // version
        input.readByte() // command
        input.readByte() // reserved
        val addressType = input.readByte().toInt() and 0xFF
        val host =
            when (addressType) {
                ATYP_IPV4 -> input.readByteArray(4).joinToString(".") { (it.toInt() and 0xFF).toString() }
                ATYP_DOMAIN_NAME -> input.readByteArray(input.readByte().toInt() and 0xFF).decodeToString()
                ATYP_IPV6 -> input.readByteArray(16).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
                else -> "unknown address type $addressType"
            }
        val portBytes = input.readByteArray(2)
        val port = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)
        connectRequests.send(SocksConnectRequest(addressType, host, port))

        output.writeByteArray(byteArrayOf(SOCKS_VERSION, REPLY_CONNECTION_REFUSED, 0, ATYP_IPV4.toByte(), 0, 0, 0, 0, 0, 0))
    }

    private data class SocksConnectRequest(
        val addressType: Int,
        val host: String,
        val port: Int,
    )

    private companion object {
        const val REQUEST_TIMEOUT_MS = 15_000L
        const val SOCKS_VERSION: Byte = 5
        const val METHOD_NO_AUTH: Byte = 0
        const val REPLY_CONNECTION_REFUSED: Byte = 5
        const val ATYP_IPV4 = 1
        const val ATYP_DOMAIN_NAME = 3
        const val ATYP_IPV6 = 4

        // Syntactically valid v3 onion address (56 base32 chars); never contacted.
        const val ONION_HOST = "bisqmobiletestbisqmobiletestbisqmobiletestbisqmobiletest.onion"

        // Base64 SHA-256 of nothing in particular; the TLS challenge is never reached.
        const val DUMMY_TLS_FINGERPRINT = "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="
    }
}
