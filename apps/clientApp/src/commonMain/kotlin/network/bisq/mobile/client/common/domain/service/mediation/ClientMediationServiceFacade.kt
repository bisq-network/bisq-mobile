package network.bisq.mobile.client.common.domain.service.mediation

import network.bisq.mobile.client.common.domain.util.notifyIfDemoModeRestricted
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager

class ClientMediationServiceFacade(
    val apiGateway: MediationApiGateway,
    private val globalUiManager: GlobalUiManager,
) : ServiceFacade(),
    MediationServiceFacade {
    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return try {
            val result = apiGateway.reportToMediator(value.tradeId)
            result.fold(
                onSuccess = { Result.success(it) },
                onFailure = { exception ->
                    when {
                        exception is WebSocketRestApiException && exception.httpStatusCode.value == 409 -> {
                            Result.failure(MediatorNotAvailableException())
                        }

                        else -> Result.failure(exception)
                    }
                },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
