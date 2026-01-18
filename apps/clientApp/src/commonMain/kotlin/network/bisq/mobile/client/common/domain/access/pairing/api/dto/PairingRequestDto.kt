package network.bisq.mobile.client.common.domain.access.pairing.api.dto

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class PairingRequestDto(
    val payload: PairingRequestPayloadDto,
    val signatureBase64: String
) {

    @OptIn(ExperimentalEncodingApi::class)
    fun signatureBytes(): ByteArray {
        require(signatureBase64.isNotBlank()) {
            "signatureBase64 must not be null or blank"
        }

        try {
            // Standard Base64 (NOT URL-safe)
            return Base64.Default.decode(signatureBase64)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("signatureBase64 is not valid Base64", e)
        }
    }
}
