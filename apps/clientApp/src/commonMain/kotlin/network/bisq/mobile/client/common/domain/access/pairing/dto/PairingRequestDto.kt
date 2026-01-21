package network.bisq.mobile.client.common.domain.access.pairing.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestDto(
    val version: Byte,
    val pairingCodeId: String,
    val clientName: String,
)
