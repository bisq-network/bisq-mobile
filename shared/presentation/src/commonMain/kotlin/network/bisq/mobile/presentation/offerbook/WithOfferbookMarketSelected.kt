package network.bisq.mobile.presentation.offerbook

import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType

/** Selects [marketListItem] as the offerbook market, then runs [onSelected]; a failed selection stays put and shows the generic error. */
fun BasePresenter.withOfferbookMarketSelected(
    offersServiceFacade: OffersServiceFacade,
    marketListItem: MarketListItem,
    onSelected: () -> Unit,
) {
    offersServiceFacade
        .selectOfferbookMarket(marketListItem)
        .onSuccess { onSelected() }
        .onFailure { e ->
            log.e(e) { "Market selection failed" }
            showSnackbar("mobile.error.generic".i18n(), type = SnackbarType.ERROR)
        }
}
