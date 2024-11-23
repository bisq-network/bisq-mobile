package network.bisq.mobile.android.node.domain.offerbook

import co.touchlab.kermit.Logger
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.market.MarketChannel
import network.bisq.mobile.android.node.domain.offerbook.market.Markets
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade

class NodeOfferbookServiceFacade(private val applicationServiceSupplier: AndroidApplicationService.Supplier) :
    OfferbookServiceFacade {

    var marketsFacade: Markets = Markets(applicationServiceSupplier)
    var marketChannel: MarketChannel = MarketChannel(applicationServiceSupplier)
    private val log = Logger.withTag(this::class.simpleName ?: "NodeOfferbookServiceFacade")
    override val markets: List<Market> get() = marketsFacade.markets

    override fun initialize() {
        marketsFacade.initialize()
        marketChannel.initialize()
    }

    override fun resume() {
        marketsFacade.resume()
        marketChannel.resume()
    }

    override fun dispose() {
        marketsFacade.dispose()
        marketChannel.dispose()
    }
}