package network.bisq.mobile.domain.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.FIAT_SCALE_FACTOR
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMaxAmount
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMinAmount
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import kotlin.math.floor
import kotlin.math.pow
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
        // The score is MINE (as prospective seller) for every BUY offer in a list, so callers
        // iterating many offers should fetch it once and pass it here — on the client a
        // getReputation call can be a full websocket round trip (debug builds bypass the local
        // cache), and fetching per offer serializes N network calls before a list can render.
        preFetchedReputation: Result<ReputationScoreVO>? = null,
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

        // Safely get seller's reputation score with proper error handling.
        // A genuinely cancelled caller (e.g. a superseded mapLatest pipeline run) must propagate its
        // cancellation - treating it as "no reputation" would log noise and wrongly cache valid offers
        // as invalid below. ensureActive() gates the rethrow so a CancellationException that does NOT
        // stem from our own cancellation (e.g. a request timeout) still falls through to the plain
        // failure handling.
        val reputationResult =
            preFetchedReputation
                ?: runCatching { reputationServiceFacade.getReputation(userProfileId) }
                    .getOrElse { Result.failure(it) }
        if (reputationResult.exceptionOrNull() is CancellationException) {
            currentCoroutineContext().ensureActive()
        }
        val myScore: Long =
            reputationResult.fold(
                onSuccess = { it.totalScore },
                onFailure = { exception ->
                    logger.d("Exception at reputationServiceFacade.getReputation", exception)
                    0L // Default to zero score on failure
                },
            )

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
        val fiatPriceQuote = marketPriceServiceFacade.findMarketPriceItem(market)?.priceQuote ?: return null
        val usdPriceQuote = marketPriceServiceFacade.findUSDMarketPriceItem()?.priceQuote ?: return null
        return getRequiredReputationScore(fiatPriceQuote, usdPriceQuote, fiat, limits)
    }

    /** The USD value of [fiatAmount], converted through BTC, rounded half up to whole USD, times the score per USD. */
    fun getRequiredReputationScore(
        fiatPriceQuote: PriceQuoteVO,
        usdPriceQuote: PriceQuoteVO,
        fiatAmount: MonetaryVO,
        limits: TradeAmountLimitsVO,
    ): Long {
        val btcAmount = fiatPriceQuote.toBaseSideMonetary(fiatAmount)
        return getRequiredReputationScoreByUsdAmount(usdPriceQuote.toQuoteSideMonetary(btcAmount), limits)
    }

    fun getRequiredReputationScoreByUsdAmount(
        usdAmount: MonetaryVO,
        limits: TradeAmountLimitsVO,
    ): Long {
        val faceValue: Double = MonetaryVO.toFaceValue(usdAmount.round(0), 0)
        return (faceValue * limits.requiredReputationScorePerUsd).roundToLong()
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
        val usdBitcoinMarket = marketPriceServiceFacade.findUSDMarketPriceItem() ?: return null
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
        val fiatPriceQuote = marketPriceServiceFacade.findMarketPriceItem(market)?.priceQuote ?: return null
        val usdPriceQuote = marketPriceServiceFacade.findUSDMarketPriceItem()?.priceQuote ?: return null
        return getReputationBasedQuoteSideAmount(fiatPriceQuote, usdPriceQuote, myReputationScore, limits)
    }

    /**
     * The USD amount allowed by [reputationScore] (see [getMaxUsdTradeAmount]), converted to the quote
     * currency and rounded to whole units, then lowered by whole units until its required score is not
     * above the score. No tolerance here: it is left for price differences between the two nodes.
     *
     * Rounding to the nearest unit instead of down keeps a whole unit that the conversion through BTC
     * reduces slightly, e.g. 6 USD becoming 5.9995 USD.
     */
    fun getReputationBasedQuoteSideAmount(
        fiatPriceQuote: PriceQuoteVO,
        usdPriceQuote: PriceQuoteVO,
        reputationScore: Long,
        limits: TradeAmountLimitsVO,
    ): MonetaryVO {
        val btcAmount = usdPriceQuote.toBaseSideMonetary(getMaxUsdTradeAmount(reputationScore, limits))
        var quoteSideAmount = fiatPriceQuote.toQuoteSideMonetary(btcAmount).round(0)
        val oneUnit = 10.0.pow(quoteSideAmount.precision).toLong()
        while (quoteSideAmount.value > 0 &&
            getRequiredReputationScore(fiatPriceQuote, usdPriceQuote, quoteSideAmount, limits) > reputationScore
        ) {
            quoteSideAmount = quoteSideAmount.withValue(quoteSideAmount.value - oneUnit)
        }
        return quoteSideAmount
    }

    /**
     * The largest amount a buyer can take from a seller's range offer: the offer's maximum, limited to
     * the seller's maximum (see [getReputationBasedQuoteSideAmount]), but not below the offer's minimum.
     * The buyer takes the offer only if the rule with its tolerance covers the minimum; the seller's
     * maximum is in whole units without tolerance, so it can be below such a minimum. All values are
     * quote-side amounts of the same currency.
     */
    fun getBuyersMaxQuoteSideAmount(
        offersMinQuoteSideAmount: Long,
        offersMaxQuoteSideAmount: Long,
        sellersMaxQuoteSideAmount: Long,
    ): Long = maxOf(minOf(sellersMaxQuoteSideAmount, offersMaxQuoteSideAmount), offersMinQuoteSideAmount)

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
        // Rounded down so that the amount never requires more than the given score
        val usdAmount = floor(reputationScore / limits.requiredReputationScorePerUsd)
        return FiatVOFactory.fromFaceValue(usdAmount, "USD")
    }

    /**
     * For a positive score, the larger of the score with the relative tolerance (rounded half up) and the
     * score plus the minimum tolerance, which covers whole-USD rounding at low scores. A zero score gets
     * no tolerance. Mirrors bisq2's rule (docs/specifications/trade/bisq-easy-seller-reputation-limit.md).
     */
    fun withTolerance(
        makersReputationScore: Long,
        limits: TradeAmountLimitsVO,
    ): Long {
        val scoreWithRelativeTolerance = (makersReputationScore * (1 + limits.tolerance)).roundToLong()
        if (makersReputationScore <= 0) {
            return scoreWithRelativeTolerance
        }
        return maxOf(scoreWithRelativeTolerance, makersReputationScore + limits.minimumReputationScoreTolerance)
    }

    fun isCoveredWithTolerance(
        reputationScore: Long,
        requiredReputationScore: Long,
        limits: TradeAmountLimitsVO,
    ): Boolean = withTolerance(reputationScore, limits) >= requiredReputationScore

    private fun MonetaryVO.withValue(newValue: Long): MonetaryVO =
        when (this) {
            is FiatVO -> copy(value = newValue)
            is CoinVO -> copy(value = newValue)
        }

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
