package network.bisq.mobile.client.service.mediation

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientMediationServiceFacade(val apiGateway: MediationApiGateway) : MediationServiceFacade, Logging {
    // API
    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> {
        return apiGateway.reportToMediator(value.tradeId)
    }
}