package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO

@Serializable
data class MarketPriceItem(val market: MarketVO, val priceQuote: PriceQuoteVO, val formattedPrice: String) :
    BaseModel() {
}