package network.bisq.mobile.presentation.ui.uicases.offers

import network.bisq.mobile.domain.data.model.offerbook.market.MarketListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class MarketListPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    ) : BasePresenter(mainPresenter) {

    private var mainCurrencies = OfferbookServiceFacade.mainCurrencies

    var marketListItemWithNumOffers: List<MarketListItem> = offerbookServiceFacade.offerbookMarketItems
        .sortedWith(
            compareByDescending<MarketListItem> {  it.numOffers.value }
                .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) } // [1]
                .thenBy { item->
                    if (!mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) item.market.quoteCurrencyName
                    else null // Null values will naturally be sorted together
                }
        )
    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    fun onSelectMarket(marketListItem: MarketListItem) {
        offerbookServiceFacade.selectMarket(marketListItem)
        rootNavigator.navigate(Routes.OfferList.name)
    }

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }
}
