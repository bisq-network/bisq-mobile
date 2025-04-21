package network.bisq.mobile.domain.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.utils.OfferUtils.getFixedOrMaxAmount
import network.bisq.mobile.domain.utils.OfferUtils.getFixedOrMinAmount
import kotlin.math.roundToLong


object BisqEasyTradeAmountLimits {
    private val invalidSellOffers: MutableSet<String> = mutableSetOf()
    private val invalidSellOffersMutex = Mutex()

    fun getMinAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val minFiatAmount = fromUsd(
            marketPriceServiceFacade,
            MarketVO("BTC", quoteCurrencyCode),
            DEFAULT_MIN_USD_TRADE_AMOUNT
        )
        return ((minFiatAmount?.value?.toDouble() ?: 0.0) / 10000).roundToLong() * 10000
    }

    fun getMaxAmountValue(marketPriceServiceFacade: MarketPriceServiceFacade, quoteCurrencyCode: String): Long {
        val maxFiatAmount = fromUsd(
            marketPriceServiceFacade,
            MarketVO("BTC", quoteCurrencyCode),
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

    suspend fun isSellOfferInvalid(
        item: OfferItemPresentationModel,
        useCache: Boolean = true,
        marketPriceServiceFacade: MarketPriceServiceFacade,
        reputationServiceFacade: ReputationServiceFacade
    ): Boolean {
        val bisqEasyOffer = item.bisqEasyOffer
        require(bisqEasyOffer.direction == DirectionEnum.SELL)

        val offerId = bisqEasyOffer.id
        if (useCache && isInvalidSellOffer(offerId)) {
            return true
        }

        val logger = getLogger("BisqEasyTradeAmountLimits")
        val requiredReputationScoreForMinOrFixed =
            findRequiredReputationScoreForMinOrFixedAmount(marketPriceServiceFacade, bisqEasyOffer)
                ?: run {
                    logger.e { "requiredReputationScoreForMinAmount is null" }
                    return false
                }

        val userProfileId = bisqEasyOffer.makerNetworkId.pubKey.id
        val sellersScore: Long = run {
            val reputationScoreResult: Result<ReputationScoreVO> = withContext(IODispatcher) {
                reputationServiceFacade.getReputation(userProfileId)
            }
            reputationScoreResult.exceptionOrNull()?.let { exception ->
                logger.e("Exception at reputationServiceFacade.getReputation", exception)
            }
            reputationScoreResult.getOrNull()?.totalScore ?: 0
        }
        val isInvalid = sellersScore < requiredReputationScoreForMinOrFixed
        if (isInvalid) {
            addInvalidSellOffer(offerId) // We also add it if cache is false
        }
        return isInvalid
    }

    fun findRequiredReputationScoreForMaxOrFixedAmount(
        marketPriceService: MarketPriceServiceFacade,
        offer: BisqEasyOfferVO
    ): Long? {
        val amount = getFixedOrMaxAmount(offer)
        val fiatAmount = FiatVOFactory.from(amount, offer.market.quoteCurrencyCode)
        return findRequiredReputationScoreByFiatAmount(marketPriceService, offer.market, fiatAmount)
    }

    fun findRequiredReputationScoreForMinOrFixedAmount(
        marketPriceService: MarketPriceServiceFacade,
        offer: BisqEasyOfferVO
    ): Long? {
        val amount = getFixedOrMinAmount(offer)
        val fiatAmount = FiatVOFactory.from(amount, offer.market.quoteCurrencyCode)
        return findRequiredReputationScoreByFiatAmount(marketPriceService, offer.market, fiatAmount)
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

    suspend fun addInvalidSellOffer(id: String) {
        invalidSellOffersMutex.withLock {
            invalidSellOffers.add(id)
        }
    }

    suspend fun isInvalidSellOffer(id: String): Boolean {
        return invalidSellOffersMutex.withLock {
            id in invalidSellOffers
        }
    }
}