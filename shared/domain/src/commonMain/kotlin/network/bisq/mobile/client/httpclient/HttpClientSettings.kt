package network.bisq.mobile.client.httpclient

import io.ktor.client.engine.ProxyBuilder
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.service.network.KmpTorService


data class HttpClientSettings(
    val apiUrl: String?,
    val selectedNetworkType: NetworkType = NetworkType.LAN,
    val proxyUrl: String? = null,
    val isTorProxy: Boolean = false,
) {
    companion object {
        suspend fun from(settings: Settings, kmpTorService: KmpTorService?): HttpClientSettings {
            val selectedNetworkType = settings.selectedNetworkType
            var isProxyUrlTor = settings.isProxyUrlTor
            var proxyUrl: String? = null
            if (selectedNetworkType == NetworkType.TOR && !settings.useExternalProxy) {
                val socksPort = if (settings.isInternalTorEnabled && kmpTorService != null) {
                    // kmpTorService.getSocksPort() // TODO: fix after kmp tor is fixed
                    "0"
                } else {
                    // we intentionally want to fail fast instead of not setting the proxy
                    "0"
                }
                proxyUrl = "127.0.0.1:$socksPort"
                isProxyUrlTor = true
            } else if (settings.useExternalProxy) {
                proxyUrl = settings.proxyUrl
            }
            return HttpClientSettings(
                settings.bisqApiUrl,
                selectedNetworkType,
                proxyUrl,
                isProxyUrlTor,
            )
        }
    }

    fun bisqProxyConfig(): BisqProxyConfig? {
        if (!proxyUrl.isNullOrBlank()) {
            val address = AddressVO.from(proxyUrl)
            if (address != null) {
                return BisqProxyConfig(
                    ProxyBuilder.socks(address.host, address.port),
                    isTorProxy
                )
            }
        }
        return null
    }
}
