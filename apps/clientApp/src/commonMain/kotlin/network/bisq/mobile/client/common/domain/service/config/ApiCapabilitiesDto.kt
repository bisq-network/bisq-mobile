package network.bisq.mobile.client.common.domain.service.config

import kotlinx.serialization.Serializable

/**
 * Response of the trusted node's `GET /config/capabilities`: the API version and the keys of the
 * recent features this node supports. Mirrors bisq2's `ApiCapabilitiesDto`.
 */
@Serializable
data class ApiCapabilitiesDto(
    val apiVersion: String,
    val features: List<String> = emptyList(),
)
