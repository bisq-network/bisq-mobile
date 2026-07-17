package network.bisq.mobile.domain.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FIAT_SCALE_FACTOR
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMaxAmount
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMinAmount
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import kotlin.math.roundToLong

object BisqEasyTradeAmountLimits {
    private val invalidBuyOffers: MutableSet<String> = mutableSetOf()
    private val invalidBuyOffersMutex = Mutex()

    fun getMinAmountValue(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        quoteCurrencyCode: String,
        limits: TradeAmountLimitsVO,
    ): Long {
        val minFiatAmount =
            fromUsd(
                marketPriceServiceFacade,
                MarketVO("BTC", quoteCurrencyCode),
                limits.defaultMinUsdTradeAmount,
            )
        // Round scaled fiat long to whole fiat units (drop sub-unit precision).
        return ((minFiatAmount?.value?.toDouble() ?: 0.0) / FIAT_SCALE_FACTOR).roundToLong() * FIAT_SCALE_FACTOR.toLong()
    }

    fun getMaxAmountValue(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        quoteCurrencyCode: String,
        limits: TradeAmountLimitsVO,
    ): Long {
        val maxFiatAmount =
            fromUsd(
                marketPriceServiceFacade,
                MarketVO("BTC", quoteCurrencyCode),
                limits.maxUsdTradeAmount,
            )
        return ((maxFiatAmount?.value?.toDouble() ?: 0.0) / FIAT_SCALE_FACTOR).roundToLong() * FIAT_SCALE_FACTOR.toLong()
    }

    fun fromUsd(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        usd: FiatVO,
    ): MonetaryVO? =
        marketPriceServiceFacade
            .findMarketPriceItem(MarketVOFactory.USD)
            ?.let { usdMarketPriceItem ->
                val defaultMinBtcTradeAmount = usdMarketPriceItem.priceQuote.toBaseSideMonetary(usd)
                val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market)
                marketPriceItem?.priceQuote?.toQuoteSideMonetary(defaultMinBtcTradeAmount)
            }

    suspend fun isBuyOfferInvalid(
        item: OfferItemPresentationModel,
        useCache: Boolean = true,
        marketPriceServiceFacade: MarketPriceServiceFacade,
        reputationServiceFacade: ReputationServiceFacade,
        userProfileId: String,
        limits: TradeAmountLimitsVO,
    ): Boolean {
        val bisqEasyOffer = item.bisqEasyOffer
        require(bisqEasyOffer.direction == DirectionEnum.BUY)

        val offerId = bisqEasyOffer.id
        if (useCache && isInvalidBuyOffer(offerId)) {
            return true
        }

        val logger = getLogger("BisqEasyTradeAmountLimits")

        // Safely get required reputation score, return false if null
        val requiredReputationScoreForMinOrFixed =
            findRequiredReputationScoreForMinOrFixedAmount(
                marketPriceServiceFacade,
                bisqEasyOffer,
                limits,
            ) ?: run {
                logger.e { "requiredReputationScoreForMinAmount is null" }
                return false
            }

        // Safely get seller's reputation score with proper error handling
        val myScore: Long =
            runCatching {
                reputationServiceFacade.getReputation(userProfileId).fold(
                    onSuccess = { it.totalScore },
                    onFailure = { exception ->
                        logger.d("Exception at reputationServiceFacade.getReputation", exception)
                        0L // Default to zero score on failure
                    },
                )
            }.getOrDefault(0L)

        val isInvalid = myScore < requiredReputationScoreForMinOrFixed
        if (isInvalid) {
            addInvalidBuyOffer(offerId) // We also add it if cache is false
        }
        return isInvalid
    }

    fun findRequiredReputationScoreForMaxOrFixedAmount(
        marketPriceService: MarketPriceServiceFacade,
        offer: BisqEasyOfferVO,
        limits: TradeAmountLimitsVO,
    ): Long? {
        val amount = offer.getFixedOrMaxAmount()
        val fiatAmount = FiatVOFactory.from(amount, offer.market.quoteCurrencyCode)
        return findRequiredReputationScoreByFiatAmount(marketPriceService, offer.market, fiatAmount, limits)
    }

    fun findRequiredReputationScoreForMinOrFixedAmount(
        marketPriceService: MarketPriceServiceFacade,
        offer: BisqEasyOfferVO,
        limits: TradeAmountLimitsVO,
    ): Long? {
        val amount = offer.getFixedOrMinAmount()
        val fiatAmount = FiatVOFactory.from(amount, offer.market.quoteCurrencyCode)
        return findRequiredReputationScoreByFiatAmount(marketPriceService, offer.market, fiatAmount, limits)
    }

    fun findRequiredReputationScoreByFiatAmount(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        fiat: MonetaryVO,
        limits: TradeAmountLimitsVO,
    ): Long? {
        val btcAmount = fiatToBtc(marketPriceServiceFacade, market, fiat) ?: return null
        val fiatAmount = btcToUsd(marketPriceServiceFacade, btcAmount) ?: return null
        return getRequiredReputationScoreByUsdAmount(fiatAmount, limits)
    }

    fun getRequiredReputationScoreByUsdAmount(
        usdAmount: MonetaryVO,
        limits: TradeAmountLimitsVO,
    ): Long {
        val value = usdAmount.round(0)
        val faceValue: Double = MonetaryVO.toFaceValue(value, 0)
        return (faceValue * limits.requiredReputationScorePerUsd).toLong()
    }

    fun fiatToBtc(
        marketPriceServiceFacade: MarketPriceServiceFacade,
        market: MarketVO,
        fiatAmount: MonetaryVO,
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
        myReputationScore: Long,
        limits: TradeAmountLimitsVO,
    ): MonetaryVO? {
        val maxUsdTradeAmount: FiatVO = getMaxUsdTradeAmount(myReputationScore, limits)
        val usdMarketPriceItem = marketPriceServiceFacade.findUSDMarketPriceItem() ?: return null
        val defaultMaxBtcTradeAmount =
            usdMarketPriceItem.priceQuote.toBaseSideMonetary(maxUsdTradeAmount)
        val marketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market)
        val finalValue = marketPriceItem?.priceQuote?.toQuoteSideMonetary(defaultMaxBtcTradeAmount)
        return finalValue
    }

    fun getMaxUsdTradeAmount(
        totalScore: Long,
        limits: TradeAmountLimitsVO,
    ): FiatVO {
        val maxAmountAllowedByReputation = getUsdAmountFromReputationScore(totalScore, limits)
        val value: Long = minOf(limits.maxUsdTradeAmount.value, maxAmountAllowedByReputation.value)
        return FiatVOFactory.from(value, "USD")
    }

    fun getUsdAmountFromReputationScore(
        reputationScore: Long,
        limits: TradeAmountLimitsVO,
    ): MonetaryVO {
        val usdAmount = reputationScore / limits.requiredReputationScorePerUsd
        return FiatVOFactory.fromFaceValue(usdAmount, "USD")
    }

    fun withTolerance(
        makersReputationScore: Long,
        limits: TradeAmountLimitsVO,
    ): Long = (makersReputationScore * (1 + limits.tolerance)).toLong()

    suspend fun addInvalidBuyOffer(id: String) {
        invalidBuyOffersMutex.withLock {
            invalidBuyOffers.add(id)
        }
    }

    suspend fun isInvalidBuyOffer(id: String): Boolean =
        invalidBuyOffersMutex.withLock {
            id in invalidBuyOffers
        }
}
