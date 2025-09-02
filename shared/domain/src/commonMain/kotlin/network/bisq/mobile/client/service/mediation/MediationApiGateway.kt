package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException
import network.bisq.mobile.domain.utils.Logging

class MediationApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) : Logging {
    private val basePath = "mediation"

    suspend fun reportToMediator(tradeId: String): Result<Unit> {
        //todo backend not impl yet
        return try {
            val result = webSocketApiClient.put<Unit, String>("$basePath/selected", tradeId)
            result.fold(
                onSuccess = { Result.success(it) },
                onFailure = { exception ->
                    // Map API errors to appropriate typed exceptions
                    when {
                        exception is WebSocketRestApiException &&
                        exception.message?.contains("no mediator", ignoreCase = true) == true -> {
                            Result.failure(MediatorNotAvailableException(exception.message ?: "No mediator available"))
                        }
                        else -> Result.failure(exception)
                    }
                }
            )
        } catch (e: Exception) {
            // Handle any other exceptions that might occur
            if (e.message?.contains("no mediator", ignoreCase = true) == true) {
                Result.failure(MediatorNotAvailableException(e.message ?: "No mediator available"))
            } else {
                Result.failure(e)
            }
        }
    }
}

