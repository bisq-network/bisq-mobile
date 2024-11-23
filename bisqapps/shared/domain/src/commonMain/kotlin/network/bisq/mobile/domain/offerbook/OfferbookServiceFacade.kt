package network.bisq.mobile.domain.offerbook

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.replicated_model.common.currency.MarketListItem
import network.bisq.mobile.domain.LifeCycleAware

interface OfferbookServiceFacade: LifeCycleAware {
    val marketListItemList: List<MarketListItem>
    val offerbookListItemList: StateFlow<List<OfferbookListItem>>
    val selectedOfferbookMarket: StateFlow<OfferbookMarket>

    fun selectMarket(marketListItem: MarketListItem)

    companion object {
        val mainCurrencies: List<String> =
            listOf("usd", "eur", "gbp", "cad", "aud", "rub", "cny", "inr", "ngn")
    }
}