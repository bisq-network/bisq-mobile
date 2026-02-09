package network.bisq.mobile.client.common.domain.sensitive_settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.utils.EMPTY_STRING

@Serializable
data class SensitiveSettings(
    val clientName: String? = null, // Set by client before pairing
    val bisqApiUrl: String = EMPTY_STRING, // Server provided at pairing
    val tlsFingerprint: String? = null, // Server provided at pairing if tls is required (clearnet)
    val clientSecret: String? = null, // Server provided at pairing
    val clientId: String? = null, // Server provided at pairing
    val sessionId: String? = null, // Server provided at pairing or at session request
    val selectedProxyOption: BisqProxyOption = BisqProxyOption.NONE,
    val externalProxyUrl: String = EMPTY_STRING,
)
