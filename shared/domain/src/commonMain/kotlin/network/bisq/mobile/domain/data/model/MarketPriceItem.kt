package network.bisq.mobile.domain.data.model

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO

data class MarketPriceItem(
    val market: MarketVO,
    val priceQuote: PriceQuoteVO,
    val formattedPrice: String
)