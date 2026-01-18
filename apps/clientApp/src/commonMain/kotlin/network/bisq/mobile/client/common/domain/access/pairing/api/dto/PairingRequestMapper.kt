package network.bisq.mobile.client.common.domain.access.pairing.api.dto

import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.access.pairing.PairingRequest
import network.bisq.mobile.client.common.domain.access.pairing.PairingRequestPayload
import network.bisq.mobile.client.common.domain.access.security.PairingCryptoUtils
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object PairingRequestMapper {
    fun toBisq2Model(dto: PairingRequestDto?): PairingRequest {
        require(dto != null) {
            "Missing pairing request payload"
        }

        val payload = toBisq2Model(dto.payload)
        return PairingRequest(
            payload = payload,
            signature = dto.signatureBytes(),
        )
    }

    private fun toBisq2Model(dto: PairingRequestPayloadDto): PairingRequestPayload =
        PairingRequestPayload(
            pairingCodeId = dto.pairingCodeId,
            clientPublicKey = decodePublicKey(dto.clientPublicKeyBase64),
            deviceName = dto.deviceName,
            timestamp = Instant.fromEpochMilliseconds(dto.timestampEpochMillis),
        )

    fun fromBisq2Model(model: PairingRequest): PairingRequestDto =
        PairingRequestDto(
            payload = fromBisq2Model(model.payload),
            signatureBase64 = Base64.Default.encode(model.signature),
        )

    private fun fromBisq2Model(model: PairingRequestPayload): PairingRequestPayloadDto =
        PairingRequestPayloadDto(
            version = 1,
            pairingCodeId = model.pairingCodeId,
            clientPublicKeyBase64 = Base64.Default.encode(model.clientPublicKey),
            deviceName = model.deviceName,
            timestampEpochMillis = model.timestamp.toEpochMilliseconds(),
        )

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodePublicKey(
        base64: String,
    ): ByteArray {
        try {
            val bytes = Base64.Default.decode(base64)
            return PairingCryptoUtils.generatePublic(bytes)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid device public key", e)
        }
    }
}
