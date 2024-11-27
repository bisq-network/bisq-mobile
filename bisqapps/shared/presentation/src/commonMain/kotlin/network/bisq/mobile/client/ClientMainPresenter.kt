package network.bisq.mobile.client

import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class ClientMainPresenter(
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    private val  marketPriceServiceFacade: MarketPriceServiceFacade
) : MainPresenter() {

    override fun onViewAttached() {
        applicationBootstrapFacade.activate()
        offerbookServiceFacade.activate()
        marketPriceServiceFacade.activate()
    }

    override fun onViewUnattaching() {
        applicationBootstrapFacade.deactivate()
        offerbookServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
    }
}