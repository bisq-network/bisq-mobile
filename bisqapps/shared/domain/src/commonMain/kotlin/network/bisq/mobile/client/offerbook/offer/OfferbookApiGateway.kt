package network.bisq.mobile.client.offerbook.offer

import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.domain.data.model.offerbook.OfferListItem
import network.bisq.mobile.utils.Logging

class OfferbookApiGateway(private val apiRequestService: ApiRequestService) : Logging {
    private val basePath = "offerbook"

    suspend fun getMarkets(): List<Market> {
        return apiRequestService.get("$basePath/markets")
    }

    suspend fun getNumOffersByMarketCode(): Map<String, Int> {
        return apiRequestService.get("$basePath/markets/offers/count")
    }

    suspend fun getOffers(code: String): List<OfferListItem> {
        return apiRequestService.get("$basePath/markets/$code/offers")
    }
}

