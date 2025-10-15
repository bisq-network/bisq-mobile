package network.bisq.mobile.client.httpclient

import io.ktor.client.engine.ProxyBuilder
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO


data class HttpClientSettings(
    val apiUrl: String?,
    val proxyUrl: String?,
    val isTorProxy: Boolean = false,
) {
    companion object {
        fun from(settings: Settings?) =
            HttpClientSettings(
                settings?.bisqApiUrl,
                settings?.proxyUrl,
                settings?.isTorProxy ?: true,
            )
    }

    fun bisqProxyConfig(): BisqProxyConfig? {
        if (!this.proxyUrl.isNullOrBlank()) {
            val address = AddressVO.from(this.proxyUrl)
            if (address != null) {
                return BisqProxyConfig(
                    ProxyBuilder.socks(address.host, address.port),
                    this.isTorProxy
                )
            }
        }
        return null
    }
}
