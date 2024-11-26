package network.bisq.mobile.client.market

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.utils.Logging

class MarketPriceApiGateway(private val apiRequestService: ApiRequestService) : Logging {
    private val basePath = "market-price"

    suspend fun getQuotes(): MarketPriceResponse {
        return apiRequestService.get("$basePath/quotes")
    }
}

@Serializable
class MarketPriceResponse(val quotes: Map<String, Long>)
