package network.bisq.mobile.client.httpclient

import io.ktor.client.engine.ProxyBuilder
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.data.model.SensitiveSettings
import network.bisq.mobile.domain.data.replicated.common.network.AddressVO
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.network.KmpTorService


data class HttpClientSettings(
    val apiUrl: String?,
    val selectedProxyOption: BisqProxyOption = BisqProxyOption.NONE,
    val proxyUrl: String? = null,
    val isTorProxy: Boolean = false,
    val password: String? = null,
) {
    companion object {
        /**
         * Warning: Suspends until [KmpTorService] is started if selected proxy option is [BisqProxyOption.INTERNAL_TOR]
         */
        suspend fun from(settings: SensitiveSettings, kmpTorService: KmpTorService): HttpClientSettings {
            val selectedProxyOption = settings.selectedProxyOption
            var proxyUrl: String?
            val isTorProxy: Boolean
            when (selectedProxyOption) {
                BisqProxyOption.INTERNAL_TOR -> {
                    val socksPort = kmpTorService.getSocksPort()
                    proxyUrl = "127.0.0.1:$socksPort"
                    isTorProxy = true
                }

                BisqProxyOption.EXTERNAL_TOR -> {
                    proxyUrl = settings.externalProxyUrl
                    isTorProxy = true
                }

                BisqProxyOption.SOCKS_PROXY -> {
                    proxyUrl = settings.externalProxyUrl
                    isTorProxy = false
                }

                BisqProxyOption.NONE -> {
                    proxyUrl = null
                    isTorProxy = false
                }
            }
            return HttpClientSettings(
                settings.bisqApiUrl,
                selectedProxyOption,
                proxyUrl,
                isTorProxy,
                settings.bisqApiPassword,
            )
        }
    }

    private fun normalizeProxyHost(value: String): String {
        return if (getPlatformInfo().type == PlatformType.IOS && value == "127.0.0.1") {
            // see https://github.com/iCepa/Tor.framework/blob/a02fe7b71737041a231f7412e0c9d4a305cd4524/Tor/Classes/Core/TORController.m#L629-L632
            "localhost"
        } else {
            value
        }
    }

    fun bisqProxyConfig(): BisqProxyConfig? {
        if (!proxyUrl.isNullOrBlank()) {
            val address = AddressVO.from(proxyUrl)
            if (address != null) {
                return BisqProxyConfig(
                    ProxyBuilder.socks(normalizeProxyHost(address.host), address.port),
                    isTorProxy
                )
            }
        }
        return null
    }
}
