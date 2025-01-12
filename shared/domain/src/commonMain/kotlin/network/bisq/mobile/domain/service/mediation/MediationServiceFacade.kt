package network.bisq.mobile.domain.service.mediation

import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.TradeItemPresentationModel

interface MediationServiceFacade : LifeCycleAware {
    suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit>
}