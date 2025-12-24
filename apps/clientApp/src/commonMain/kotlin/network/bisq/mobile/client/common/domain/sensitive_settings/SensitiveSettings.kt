package network.bisq.mobile.client.common.domain.sensitive_settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption

@Serializable
data class SensitiveSettings(
    val bisqApiUrl: String = "",
    val selectedProxyOption: BisqProxyOption = BisqProxyOption.NONE,
    val externalProxyUrl: String = "",
    val bisqApiPassword: String = "",
)
