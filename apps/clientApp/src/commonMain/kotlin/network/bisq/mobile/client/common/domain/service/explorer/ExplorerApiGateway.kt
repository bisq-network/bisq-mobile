package network.bisq.mobile.client.common.domain.service.explorer

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class ExplorerApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "explorer"

    suspend fun getSelectedBlockExplorer(): Result<Map<String, String>> = webSocketApiClient.get("$basePath/selected")

    suspend fun requestTx(txId: String): Result<ExplorerTxDto> = webSocketApiClient.get("$basePath/tx/$txId")
}
