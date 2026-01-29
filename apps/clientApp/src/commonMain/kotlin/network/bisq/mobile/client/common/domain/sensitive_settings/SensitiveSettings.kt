package network.bisq.mobile.client.common.domain.sensitive_settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption

@Serializable
data class SensitiveSettings(
    val clientName: String? = null, // Set by client before pairing
    val bisqApiUrl: String = "", // Server provided at pairing
    val tlsFingerprint: String? = null, // Server provided at pairing if tls is required (clearnet)
    val clientSecret: String? = null, // Server provided at pairing
    val clientId: String? = null, // Server provided at pairing
    val sessionId: String? = null, // Server provided at pairing or at session request
    val selectedProxyOption: BisqProxyOption = BisqProxyOption.NONE,
    val externalProxyUrl: String = "",
)
