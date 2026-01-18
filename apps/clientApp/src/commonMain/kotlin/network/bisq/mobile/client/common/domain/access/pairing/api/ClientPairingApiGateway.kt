package network.bisq.mobile.client.common.domain.access.pairing.api

import network.bisq.mobile.client.common.domain.access.pairing.api.dto.PairingRequestDto
import network.bisq.mobile.client.common.domain.access.pairing.api.dto.PairingResponseDto
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class ClientPairingApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "pairing"

    suspend fun requestPairing(request: PairingRequestDto): Result<PairingResponseDto> {
        return webSocketApiClient.post(basePath, request)
    }
}
