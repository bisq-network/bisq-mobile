package network.bisq.mobile.client.common.domain.service.market

import kotlinx.serialization.Serializable

@Serializable
data class QuotesResponse(
    val quotes: Map<String, network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO>,
)
