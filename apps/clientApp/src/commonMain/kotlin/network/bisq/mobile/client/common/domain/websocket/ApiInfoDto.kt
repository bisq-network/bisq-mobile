package network.bisq.mobile.client.common.domain.websocket

import kotlinx.serialization.Serializable

/**
 * Response of the trusted node's `GET /info`, read once in the connect handshake. Mirrors bisq2's
 * `ApiInfoDto`. [features] is null when the node predates `/info` and the handshake fell back to
 * `/settings/version`; an empty list is a real answer.
 */
@Serializable
data class ApiInfoDto(
    val version: String,
    val imageVersion: String? = null,
    val features: List<String>? = null,
)
