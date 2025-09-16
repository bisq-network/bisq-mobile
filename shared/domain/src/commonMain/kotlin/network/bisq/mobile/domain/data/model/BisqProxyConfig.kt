package network.bisq.mobile.domain.data.model

import io.ktor.client.engine.ProxyConfig

data class BisqProxyConfig(
    val config: ProxyConfig,
    val isTorProxy: Boolean, // for future general proxy support
)
