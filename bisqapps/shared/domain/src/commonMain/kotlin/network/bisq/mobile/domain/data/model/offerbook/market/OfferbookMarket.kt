package network.bisq.mobile.domain.data.model.offerbook.market

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.domain.data.model.BaseModel

/**
 * Provides data for the offerbook header showing the selected market data
 */
@Serializable
data class OfferbookMarket(
    val market: Market
) : BaseModel() {
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
        val EMPTY: OfferbookMarket = OfferbookMarket(Market.EMPTY)
    }
}