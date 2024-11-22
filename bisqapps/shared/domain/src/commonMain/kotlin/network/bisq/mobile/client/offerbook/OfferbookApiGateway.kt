package network.bisq.mobile.domain.client.main.user_profile

import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.service.ApiRequestService

class OfferbookApiGateway(
    private val apiRequestService: ApiRequestService
) {
    private val log = Logger.withTag(this::class.simpleName ?: "UserProfileApiGateway")
    private val basePath = "offerbook"

    suspend fun getMarkets(): List<MarketDto> {
        return apiRequestService.get("$basePath/markets")
    }

    suspend fun getNumOffersByMarketCode(): Map<String, Int> {
        return apiRequestService.get("$basePath/markets/offers/count")
    }
}

@Serializable
class MarketDto(
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    val baseCurrencyName: String,
    val quoteCurrencyName: String
)