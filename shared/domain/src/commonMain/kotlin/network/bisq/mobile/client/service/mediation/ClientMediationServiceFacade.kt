package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.client.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException

class ClientMediationServiceFacade(val apiGateway: MediationApiGateway) : ServiceFacade(), MediationServiceFacade {
    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> {
        return try {
            val result = apiGateway.reportToMediator(value.tradeId)
            result.fold(
                onSuccess = { Result.success(it) },
                onFailure = { exception ->
                    // Map API errors to appropriate typed exceptions for consistency with NodeMediationServiceFacade
                    when {
                        exception is WebSocketRestApiException &&
                                exception.message?.contains("no mediator", ignoreCase = true) == true -> {
                            Result.failure(MediatorNotAvailableException(exception.message ?: ""))
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