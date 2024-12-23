package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.common.currency.Markets
import network.bisq.mobile.domain.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.common.monetary.fromFaceValue
import network.bisq.mobile.domain.replicated.common.monetary.toBaseSideMonetary
import network.bisq.mobile.domain.replicated.common.monetary.toQuoteSideMonetary
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import kotlin.math.roundToLong


object BisqEasyTradeAmountLimits {
    fun getMinAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val value = fromUsd(
            marketPriceServiceFacade,
            MarketVO("BTC", quoteCurrencyCode),
            DEFAULT_MIN_USD_TRADE_AMOUNT
        )?.value ?: 0
        return (value.toDouble() / 10000).roundToLong() * 10000
    }

    fun getMaxAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val value = fromUsd(
            marketPriceServiceFacade,
            MarketVO("BTC", quoteCurrencyCode),
            MAX_USD_TRADE_AMOUNT
        )?.value ?: 0
        return (value.toDouble() / 10000).roundToLong() * 10000
    }

    val DEFAULT_MIN_USD_TRADE_AMOUNT: FiatVO = FiatVO.fromFaceValue(6.0, "USD")
    val MAX_USD_TRADE_AMOUNT: FiatVO = FiatVO.fromFaceValue(600.0, "USD")

    fun fromUsd(marketPriceServiceFacade: MarketPriceServiceFacade, market: MarketVO, usd: FiatVO): MonetaryVO? {
        return marketPriceServiceFacade.findMarketPriceItem(Markets.USD)?.let { usdMarketPriceItem ->
            val defaultMinBtcTradeAmount = usdMarketPriceItem.priceQuote.toBaseSideMonetary(usd)
            val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market)
            marketPriceItem?.priceQuote?.toQuoteSideMonetary(defaultMinBtcTradeAmount)
        }
    }

}