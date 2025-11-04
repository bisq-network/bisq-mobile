package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.httpclient.BisqProxyOption

@Serializable
data class SensitiveSettings (
    val bisqApiUrl: String = "",
    val selectedProxyOption: BisqProxyOption = BisqProxyOption.NONE,
    val externalProxyUrl: String = "",
    val bisqApiPassword: String = "",
)