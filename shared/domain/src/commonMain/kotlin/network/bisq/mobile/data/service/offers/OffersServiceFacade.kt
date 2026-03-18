package network.bisq.mobile.data.service.offers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.data.service.ServiceFacade

abstract class OffersServiceFacade :
    ServiceFacade(),
    LifeCycleAware {
    protected val _offerbookListItems = MutableStateFlow<List<OfferItemPresentationModel>>(emptyList())
    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> get() = _offerbookListItems

    protected val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    protected val _offerbookMarketItems = MutableStateFlow<List<MarketListItem>>(emptyList())
    val offerbookMarketItems: StateFlow<List<MarketListItem>> get() = _offerbookMarketItems

    // Loading indicator for offerbook data fetch/select cycles
    protected val _isOfferbookLoading = MutableStateFlow(false)
    val isOfferbookLoading: StateFlow<Boolean> get() = _isOfferbookLoading

    val sortedOfferbookMarketItems: StateFlow<List<MarketListItem>> =
        offerbookMarketItems
            .map { list ->
                list.sortedWith(
                    compareByDescending<MarketListItem> { it.numOffers }
                        .thenByDescending { mainCurrencies.contains(it.market.quoteCurrencyCode.uppercase()) }
                        .thenBy { item ->
                            if (!mainCurrencies.contains(item.market.quoteCurrencyCode.uppercase())) {
                                item.localeFiatCurrencyName.ifBlank { item.market.quoteCurrencyName }
                            } else {
                                null
                            }
                        },
                )
            }.stateIn(
                serviceScope,
                SharingStarted.WhileSubscribed(5_000, 10_000),
                emptyList(),
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

    abstract fun selectOfferbookMarket(marketListItem: MarketListItem): Result<Unit>

    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    companion object {
        // Debounce configuration for market price ticks
        // Chosen to balance responsiveness and avoiding UI churn
        protected const val MARKET_TICK_DEBOUNCE_MS = 150L

        val mainCurrencies: List<String> = listOf("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "CNY", "INR", "NGN")

        fun isTerminalState(tradeState: BisqEasyTradeStateEnum): Boolean =
            when (tradeState) {
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
