package network.bisq.mobile.client.network

import io.ktor.client.engine.ProxyBuilder
import network.bisq.mobile.domain.data.model.BisqProxyConfig
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO


data class HttpClientSettings(
    val apiUrl: String?,
    val proxyUrl: String?,
    val isExternalProxyTor: Boolean = false,
) {
    companion object {
        fun from(settings: Settings?) =
            HttpClientSettings(
                settings?.bisqApiUrl,
                settings?.proxyUrl,
                settings?.isExternalProxyTor ?: false,
            )
    }

    fun bisqProxyConfig(): BisqProxyConfig? {
        // isExternalProxyTor is for future non tor proxy support
        if (this.proxyUrl?.isBlank() == false && this.isExternalProxyTor) {
            val address = AddressVO.from(this.proxyUrl)
            if (address != null) {
                return BisqProxyConfig(
                    ProxyBuilder.socks(address.host, address.port),
                    this.isExternalProxyTor
                )
            }
        }
        return null
    }
}