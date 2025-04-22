package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade

class ClientMediationServiceFacade(val apiGateway: MediationApiGateway) : ServiceFacade(), MediationServiceFacade {
    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> {
        return apiGateway.reportToMediator(value.tradeId)
    }
}