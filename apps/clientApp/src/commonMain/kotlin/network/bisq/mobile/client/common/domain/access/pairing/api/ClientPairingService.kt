package network.bisq.mobile.client.common.domain.access.pairing.api

import network.bisq.mobile.client.common.domain.access.pairing.PairingRequest
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.api.dto.PairingRequestMapper
import network.bisq.mobile.domain.utils.Logging


class ClientPairingService(
    private val apiGateway: ClientPairingApiGateway,
) : Logging {

    suspend fun requestPairing(pairingRequest: PairingRequest): Result<PairingResponse> {
        val pairingRequestDto = PairingRequestMapper.fromBisq2Model(pairingRequest)
        val result = apiGateway.requestPairing(pairingRequestDto)

        return if (result.isSuccess) {
            val dto = result.getOrThrow()
            Result.success(
                PairingResponse(
                    dto.sessionId,
                    dto.expiresAt
                )
            )
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }
}
