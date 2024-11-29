package network.bisq.mobile.domain.data.model

import network.bisq.mobile.client.replicated_model.common.currency.Market

data class MarketPriceItem(val market: Market, val quote: Long, val formattedPrice: String) :
    BaseModel() {
    companion object {
        val EMPTY: MarketPriceItem = MarketPriceItem(Market.EMPTY, 0, "")
    }
}