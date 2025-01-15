package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import kotlin.math.roundToLong


object BisqEasyTradeAmountLimits {
    fun getMinAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val value = fromUsd(
            marketPriceServiceFacade,
            network.bisq.mobile.domain.data.replicated.common.currency.MarketVO("BTC", quoteCurrencyCode),
            DEFAULT_MIN_USD_TRADE_AMOUNT
        )?.value ?: 0
        return (value.toDouble() / 10000).roundToLong() * 10000
    }

    fun getMaxAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val value = fromUsd(
            marketPriceServiceFacade,
            network.bisq.mobile.domain.data.replicated.common.currency.MarketVO("BTC", quoteCurrencyCode),
            MAX_USD_TRADE_AMOUNT
        )?.value ?: 0
        return (value.toDouble() / 10000).roundToLong() * 10000
    }

    val DEFAULT_MIN_USD_TRADE_AMOUNT: FiatVO = FiatVOFactory.fromFaceValue(6.0, "USD")
    val MAX_USD_TRADE_AMOUNT: FiatVO = FiatVOFactory.fromFaceValue(600.0, "USD")

    fun fromUsd(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        usd: FiatVO
    ): MonetaryVO? {
        return marketPriceServiceFacade.findMarketPriceItem(MarketVOFactory.USD)
            ?.let { usdMarketPriceItem ->
                val defaultMinBtcTradeAmount = usdMarketPriceItem.priceQuote.toBaseSideMonetary(usd)
                val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market)
                marketPriceItem?.priceQuote?.toQuoteSideMonetary(defaultMinBtcTradeAmount)
            }
    }

}