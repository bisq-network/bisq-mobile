package network.bisq.mobile.domain.service.offers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.ServiceFacade

abstract class OffersServiceFacade : ServiceFacade(), LifeCycleAware {

    protected val _offerbookListItems = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> get() = _offerbookListItems

    protected val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    protected val _offerbookMarketItems = MutableStateFlow<List<MarketListItem>>(emptyList())
    val offerbookMarketItems: StateFlow<List<MarketListItem>> get() = _offerbookMarketItems

    val sortedOfferbookMarketItems: StateFlow<List<MarketListItem>> = offerbookMarketItems.map { list -> list.sortedWith(
        compareByDescending<MarketListItem> { it.numOffers }
            .thenByDescending { OffersServiceFacade.mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) }
            .thenBy { item ->
                if (!OffersServiceFacade.mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) item.market.quoteCurrencyName
                else null
            }
    )}.stateIn(
        serviceScope,
        SharingStarted.WhileSubscribed(5_000, 10_000),
        emptyList()
    )

    abstract suspend fun deleteOffer(offerId: String): Result<Boolean>

    abstract suspend fun createOffer(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>,
    ): Result<String>

    abstract suspend fun createOfferWithMediatorWait(
        direction: DirectionEnum,
        market: MarketVO,
        bitcoinPaymentMethods: Set<String>,
        fiatPaymentMethods: Set<String>,
        amountSpec: AmountSpecVO,
        priceSpec: PriceSpecVO,
        supportedLanguageCodes: Set<String>,
    ): Result<String>

    abstract fun selectOfferbookMarket(marketListItem: MarketListItem)


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