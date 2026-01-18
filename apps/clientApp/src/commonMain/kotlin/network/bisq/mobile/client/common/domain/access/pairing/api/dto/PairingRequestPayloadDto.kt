package network.bisq.mobile.client.common.domain.access.pairing.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestPayloadDto(
    val version: Byte,
    val pairingCodeId: String,
    val clientPublicKeyBase64: String,
    val deviceName: String,
    val timestampEpochMillis: Long
)
