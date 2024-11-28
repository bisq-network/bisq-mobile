package network.bisq.mobile.domain.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.replicated_model.common.currency.Market

/**
 * Provides market price data
 */
data class MarketPriceItem(val market: Market) : BaseModel() {
    private val _quote = MutableStateFlow(0L)
    val quote: StateFlow<Long> get() = _quote
    fun setQuote(value: Long) {
        _quote.value = value
    }

    private val _formattedPrice = MutableStateFlow("")
    val formattedPrice: StateFlow<String> get() = _formattedPrice
    fun setFormattedPrice(value: String) {
        _formattedPrice.value = value
    }

    companion object {
        val EMPTY: MarketPriceItem = MarketPriceItem(Market.EMPTY)
    }
}