package network.bisq.mobile.data.replicated.common.currency

object MarketVOExtensions {
    val MarketVO.marketCodes: String get() = "$baseCurrencyCode/$quoteCurrencyCode"
}
