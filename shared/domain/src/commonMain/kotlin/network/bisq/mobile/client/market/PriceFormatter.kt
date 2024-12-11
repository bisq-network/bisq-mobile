package network.bisq.mobile.client.market

import network.bisq.mobile.client.replicated_model.common.currency.Market
import kotlin.math.pow
import kotlin.math.round

fun formatMarketPrice(market: Market, quote: Long): String {
    val doubleValue: Double = quote.toDouble() / 10000
    val stringValue: String = doubleValue.roundTo(2).toString()
    return stringValue + " " + market.marketCodes
}

fun Double.roundTo(places: Int): Double {
    require(places >= 0) { "Decimal places must be non-negative" }
    val factor = 10.0.pow(places)
    return round(this * factor) / factor
}