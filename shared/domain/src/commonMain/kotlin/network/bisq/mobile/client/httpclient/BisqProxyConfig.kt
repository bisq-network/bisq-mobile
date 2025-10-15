package network.bisq.mobile.client.httpclient

import io.ktor.client.engine.ProxyConfig

data class BisqProxyConfig(
    val config: ProxyConfig,
    val isTorProxy: Boolean, // for general proxy support (if decided in future)
)