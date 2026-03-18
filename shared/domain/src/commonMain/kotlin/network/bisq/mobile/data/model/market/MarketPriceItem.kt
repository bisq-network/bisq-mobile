package network.bisq.mobile.data.model.market

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO

data class MarketPriceItem(
    val market: MarketVO,
    val priceQuote: PriceQuoteVO,
    val formattedPrice: String,
)
