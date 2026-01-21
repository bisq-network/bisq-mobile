package network.bisq.mobile.client.common.domain.access.pairing

import network.bisq.mobile.client.common.domain.access.pairing.dto.PairingRequestDto
import network.bisq.mobile.client.common.domain.access.pairing.dto.PairingResponseDto
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.domain.utils.Logging

class PairingApiGateway(
    private val httpClientService: HttpClientService,
) : Logging {
    val basePath = "access"

    suspend fun requestPairing(
        request: PairingRequestDto,
    ): Result<PairingResponseDto> {
        val path = "$basePath/pairing"
        return httpClientService.post(path, request)
    }
}
