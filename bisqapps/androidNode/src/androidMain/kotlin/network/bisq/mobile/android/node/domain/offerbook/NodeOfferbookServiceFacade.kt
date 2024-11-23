package network.bisq.mobile.android.node.domain.offerbook

import co.touchlab.kermit.Logger
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.market.SelectedMarket
import network.bisq.mobile.android.node.domain.offerbook.market.Markets
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade

class NodeOfferbookServiceFacade(private val applicationServiceSupplier: AndroidApplicationService.Supplier) :
    OfferbookServiceFacade {

    var marketsFacade: Markets = Markets(applicationServiceSupplier)
    var selectedMarket: SelectedMarket = SelectedMarket(applicationServiceSupplier)
    private val log = Logger.withTag(this::class.simpleName ?: "NodeOfferbookServiceFacade")
    override val markets: List<Market> get() = marketsFacade.markets

    override fun initialize() {
        marketsFacade.initialize()
        selectedMarket.initialize()
    }

    override fun resume() {
        marketsFacade.resume()
        selectedMarket.resume()
    }

    override fun selectMarket(market: network.bisq.mobile.client.replicated_model.common.currency.Market) {
        val _market: bisq.common.currency.Market = bisq.common.currency.Market(
            market.baseCurrencyCode,
            market.quoteCurrencyCode,
            market.baseCurrencyName, market.quoteCurrencyName
        )
        selectedMarket.selectMarket(_market)
    }

    override fun dispose() {
        marketsFacade.dispose()
        selectedMarket.dispose()
    }
}