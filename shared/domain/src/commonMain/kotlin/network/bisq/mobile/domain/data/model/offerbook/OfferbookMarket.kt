package network.bisq.mobile.domain.data.model.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory

/**
 * Provides data for the offerbook header showing the selected market data
 */
// todo Not used yet, maybe we can remove it later
data class OfferbookMarket(val market: MarketVO) {
    private val _formattedPrice = MutableStateFlow("")
    val formattedPrice: StateFlow<String> get() = _formattedPrice

    fun setFormattedPrice(value: String) {
        _formattedPrice.value = value
    }

    override fun toString(): String {
        return "OfferbookMarket(\n" +
                "market='$market'\n" +
                "formattedPrice='${formattedPrice.value}'\n" +
                ")"
    }

    companion object {
        val EMPTY: OfferbookMarket = OfferbookMarket(MarketVOFactory.EMPTY)
    }
}