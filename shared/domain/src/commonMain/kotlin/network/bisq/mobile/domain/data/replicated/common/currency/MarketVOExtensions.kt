package network.bisq.mobile.domain.data.replicated.common.currency

object MarketVOExtensions {
    val MarketVO.marketCodes: String get() = "$baseCurrencyCode/$quoteCurrencyCode"
}
