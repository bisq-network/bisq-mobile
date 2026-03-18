package network.bisq.mobile.data.service.mediation

import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.LifeCycleAware

interface MediationServiceFacade : LifeCycleAware {
    suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit>
}
