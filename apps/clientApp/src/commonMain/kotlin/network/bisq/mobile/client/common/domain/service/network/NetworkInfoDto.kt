package network.bisq.mobile.client.common.domain.service.network

import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfoDto(
    val allDataReceived: Boolean,
    val torRunning: Boolean,
    val myAddress: String? = null,
    val keyId: String? = null,
    val connections: List<ConnectionDto> = emptyList(),
)

@Serializable
data class ConnectionDto(
    val connectionId: String,
    val address: String,
    val outbound: Boolean,
    val seed: Boolean,
    val establishedAtMillis: Long,
)
