package network.bisq.mobile.domain.service.offers

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum

interface OffersServiceFacade : LifeCycleAware {
    val offerbookMarketItems: StateFlow<List<MarketListItem>>
    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>>
    val selectedOfferbookMarket: StateFlow<OfferbookMarket>

    suspend fun deleteOffer(offerId: String): Result<Boolean>

    suspend fun createOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>,
    ): Result<String>

    fun selectOfferbookMarket(marketListItem: MarketListItem)

    fun getSortedOfferbookMarketItems(): List<MarketListItem> = offerbookMarketItems.value
        .sortedWith(
            compareByDescending<MarketListItem> { it.numOffers.value }
                .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) } // [1]
                .thenBy { item ->
                    if (!mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) item.market.quoteCurrencyName
                    else null // Null values will naturally be sorted together
                }
        )

    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    companion object {
        val mainCurrencies: List<String> = listOf("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "CNY", "INR", "NGN")

        fun isTerminalState(tradeState: BisqEasyTradeStateEnum): Boolean {
            return when (tradeState) {
                BisqEasyTradeStateEnum.BTC_CONFIRMED,
                BisqEasyTradeStateEnum.CANCELLED,
                BisqEasyTradeStateEnum.FAILED,
                BisqEasyTradeStateEnum.FAILED_AT_PEER,
                BisqEasyTradeStateEnum.REJECTED,
                BisqEasyTradeStateEnum.PEER_REJECTED,
                    -> true
                else -> false
            }
        }
    }
}