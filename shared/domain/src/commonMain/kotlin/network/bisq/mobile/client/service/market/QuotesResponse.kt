package network.bisq.mobile.client.service.market

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO

@Serializable
data class QuotesResponse(val quotes: Map<String, PriceQuoteVO>)