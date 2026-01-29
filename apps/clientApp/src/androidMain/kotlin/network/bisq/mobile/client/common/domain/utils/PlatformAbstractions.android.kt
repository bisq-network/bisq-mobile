package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import network.bisq.mobile.client.common.domain.access.security.TlsTrustManager
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig
import network.bisq.mobile.client.httpclient.NoDns
import network.bisq.mobile.domain.utils.getLogger
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

actual fun createHttpClient(
    host: String,
    tlsFingerprint: String?,
    proxyConfig: BisqProxyConfig?,
    config: HttpClientConfig<*>.() -> Unit,
) = HttpClient(OkHttp) {
    config(this)
    install(WebSockets)
    engine {
        proxy = proxyConfig?.config
        config {
            if (proxyConfig?.isTorProxy == true) {
                dns(NoDns())
            }
            pingInterval(15, TimeUnit.SECONDS)

            tlsFingerprint?.let {
                try {
                    val tlsTrustManager = TlsTrustManager(host, tlsFingerprint)

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(
                        null,
                        arrayOf(tlsTrustManager),
                        SecureRandom(),
                    )

                    sslSocketFactory(sslContext.socketFactory, tlsTrustManager)

                    // We verify host in the TrustManager, thus we can return always true in hostnameVerifier
                    hostnameVerifier { hostname, session -> true }
                } catch (e: Exception) {
                    getLogger("").e { "Error applying SSLContext $tlsFingerprint" }
                    throw e
                }
            }
        }
    }
}
