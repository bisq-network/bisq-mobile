package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.flow.Flow
import network.bisq.mobile.domain.service.trades.TakeOfferStatus

data class TakeOfferFlowResult(
    val statusFlow: Flow<TakeOfferStatus?>,
    val errorMessageFlow: Flow<String?>
)