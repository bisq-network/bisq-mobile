package network.bisq.mobile.client.common.domain.access.pairing

import network.bisq.mobile.client.common.domain.access.pairing.dto.PairingRequestDto
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

class PairingService(
    private val apiGateway: PairingApiGateway,
) : ServiceFacade(),
    Logging {
    companion object {
        const val VERSION: Byte = 1
    }

    suspend fun requestPairing(
        pairingCodeId: String,
        clientName: String,
    ): Result<PairingResponse> {
        val pairingRequestDto =
            PairingRequestDto(
                version = VERSION,
                pairingCodeId = pairingCodeId,
                clientName = clientName,
            )

        val result = apiGateway.requestPairing(pairingRequestDto)

        return if (result.isSuccess) {
            val dto = result.getOrThrow()
            Result.success(
                PairingResponse(
                    dto.version,
                    dto.clientId,
                    dto.clientSecret,
                    dto.sessionId,
                    dto.sessionExpiryDate,
                ),
            )
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }
}
