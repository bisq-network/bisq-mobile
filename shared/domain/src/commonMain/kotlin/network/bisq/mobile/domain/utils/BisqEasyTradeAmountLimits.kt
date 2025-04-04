package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.domain.data.replicated.common.roundDouble
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import kotlin.math.roundToLong


object BisqEasyTradeAmountLimits {
    fun getMinAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val minFiatAmount = fromUsd(
            marketPriceServiceFacade,
            network.bisq.mobile.domain.data.replicated.common.currency.MarketVO("BTC", quoteCurrencyCode),
            DEFAULT_MIN_USD_TRADE_AMOUNT
        )
        return ((minFiatAmount?.value?.toDouble() ?: 0.0) / 10000).roundToLong() * 10000
    }

    fun getMaxAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val maxFiatAmount = fromUsd(
            marketPriceServiceFacade,
            network.bisq.mobile.domain.data.replicated.common.currency.MarketVO("BTC", quoteCurrencyCode),
            MAX_USD_TRADE_AMOUNT
        )
        return ((maxFiatAmount?.value?.toDouble() ?: 0.0) / 10000).roundToLong() * 10000
    }

    val TOLERANCE: Double = 0.05
    val DEFAULT_MIN_USD_TRADE_AMOUNT: FiatVO = FiatVOFactory.fromFaceValue(6.0, "USD")
    val MAX_USD_TRADE_AMOUNT: FiatVO = FiatVOFactory.fromFaceValue(600.0, "USD")
    val REQUIRED_REPUTATION_SCORE_PER_USD: Double = 200.0

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

    fun findRequiredReputationScoreByFiatAmount(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        fiat: MonetaryVO
    ): Long? {
        val btcAmount = fiatToBtc(marketPriceServiceFacade, market, fiat) ?: return null
        val fiatAmount = btcToUsd(marketPriceServiceFacade, btcAmount) ?: return null
        return getRequiredReputationScoreByUsdAmount(fiatAmount)
    }

    fun getRequiredReputationScoreByUsdAmount(usdAmount: MonetaryVO): Long {
        val value = usdAmount.round(0)
        val faceValue: Double = MonetaryVO.toFaceValue(value, 0);
        return (faceValue * REQUIRED_REPUTATION_SCORE_PER_USD).toLong()
    }

    fun fiatToBtc(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        fiatAmount: MonetaryVO
    ): MonetaryVO? {
        val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market) ?: return null
        val btcAmount = marketPriceItem.priceQuote.toBaseSideMonetary(fiatAmount)
        return btcAmount
    }

    fun btcToUsd(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        btcAmount: MonetaryVO,
    ): MonetaryVO? {
        val usdBitcoinMarket = marketPriceServiceFacade.findUSDMarketPriceItem()!!
        return btcToFiat(marketPriceServiceFacade, usdBitcoinMarket.market, btcAmount)
    }

    fun btcToFiat(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        btcAmount: MonetaryVO,
    ): MonetaryVO? {
        val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market) ?: return null
        val fiatAmount = marketPriceItem.priceQuote.toQuoteSideMonetary(btcAmount)
        return fiatAmount
    }

    fun getReputationBasedQuoteSideAmount(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        myReputationScore: Long
    ): MonetaryVO? {
        val maxUsdTradeAmount: FiatVO = getMaxUsdTradeAmount(myReputationScore)
        val usdMarketPriceItem = marketPriceServiceFacade.findUSDMarketPriceItem() ?: return null
        val defaultMaxBtcTradeAmount = usdMarketPriceItem.priceQuote.toBaseSideMonetary(maxUsdTradeAmount)
        val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market)
        val finalValue = marketPriceItem?.priceQuote?.toQuoteSideMonetary(defaultMaxBtcTradeAmount)
        return finalValue
    }

    fun getMaxUsdTradeAmount(totalScore: Long): FiatVO {
        val maxAmountAllowedByReputation = getUsdAmountFromReputationScore(totalScore);
        val value: Double = minOf(MAX_USD_TRADE_AMOUNT.value, maxAmountAllowedByReputation.value).toDouble();
        return FiatVOFactory.from(value.toLong(), "USD");
    }

    fun getUsdAmountFromReputationScore(reputationScore: Long): MonetaryVO {
        val usdAmount = reputationScore / REQUIRED_REPUTATION_SCORE_PER_USD
        return FiatVOFactory.fromFaceValue(usdAmount, "USD")
    }

    fun withTolerance(makersReputationScore: Long): Long {
        return (makersReputationScore * (1 + TOLERANCE)).toLong();
    }

}