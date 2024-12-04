package network.bisq.mobile.presentation.ui.uicases.trades

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

// TODO: Should do Interface for this?
open class TradeFlowPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), ITradeFlowPresenter {

    override val offerListItems: StateFlow<List<OfferListItem>> = offerbookServiceFacade.offerListItems

    override val steps = listOf(
        TradeFlowScreenSteps.ACCOUNT_DETAILS.title,
        TradeFlowScreenSteps.FIAT_PAYMENT.title,
        TradeFlowScreenSteps.BITCOIN_TRANSFER.title,
        TradeFlowScreenSteps.TRADE_COMPLETED.title
    )

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }
}
