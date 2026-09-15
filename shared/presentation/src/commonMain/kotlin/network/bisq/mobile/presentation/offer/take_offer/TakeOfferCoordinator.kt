package network.bisq.mobile.presentation.offer.take_offer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.account.payment_method.BitcoinPaymentRailEnum
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.data.replicated.common.monetary.CoinVOFactory.bitcoinFrom
import network.bisq.mobile.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMaxAmount
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVOExtensions.getFixedOrMinAmount
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.repository.PayoutAddressPrepRepository
import network.bisq.mobile.domain.service.trades.ExpectedTradeProtocolRejection
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks

/**
 * Coordinates the multi-step "Take Offer" wizard flow.
 *
 * This is NOT a presenter — it does not extend [BasePresenter], has no lifecycle methods, and
 * does not interact with UI directly. It is a singleton data coordinator that:
 *
 * 1. Holds the [TakeOfferModel] shared across all wizard step screens
 * 2. Computes how many steps the wizard needs (based on the offer's payment methods and amount range)
 * 3. Provides [commit] methods for each step presenter to save user selections
 * 4. Delegates trade execution to [TradesServiceFacade]
 *
 * Usage:
 * - Injected as a Koin `single` into step presenters and screens
 * - [selectOfferToTake] must be called before navigating to the first wizard step
 *   (typically from [OfferbookPresenter])
 * - Each step presenter calls the appropriate `commit*()` method when the user advances
 * - The final step presenter calls [takeOffer] to submit
 *
 * The calling presenter is responsible for any presentation concerns (error snackbars,
 * navigation, loading states) — this coordinator only manages data and service calls.
 */
class TakeOfferCoordinator(
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val configServiceFacade: ConfigServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val payoutAddressPrepRepository: PayoutAddressPrepRepository,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Logging {
    class TakeOfferModel {
        lateinit var offerItemPresentationVO: OfferItemPresentationModel
        var hasMultipleQuoteSidePaymentMethods: Boolean = false
        var hasMultipleBaseSidePaymentMethods: Boolean = false
        var hasAmountRange: Boolean = false
        lateinit var originalPriceQuote: PriceQuoteVO
        lateinit var priceQuote: PriceQuoteVO
        lateinit var quoteAmount: FiatVO
        lateinit var baseAmount: CoinVO
        lateinit var quoteSidePaymentMethod: String
        lateinit var baseSidePaymentMethod: String

        /** Payout address collected in the optional wizard step; blank when skipped or not shown. */
        var btcAddress: String = ""

        /** The taker's profile had no completed trade when the wizard started. */
        var isFirstTimeTrader: Boolean = false

        // SELL offer: the maker sells, so the taker receives the bitcoin.
        val takerIsBuyer: Boolean
            get() = offerItemPresentationVO.bisqEasyOffer.direction == DirectionEnum.SELL
    }

    var totalSteps: Int = 1

    lateinit var takeOfferModel: TakeOfferModel

    // Whether totalSteps currently includes the payout-address step. Tracked separately because
    // the step's presence can flip with the settlement choice (see commitSettlementMethod).
    private var btcAddressStepCounted = false

    /**
     * [takerProfileId] identifies the profile taking the offer, so the optional payout-address
     * step can be offered only to first-time traders. Awaits the profile's history warm-up
     * before reading the flag — normally already finished in the background by the time the
     * user taps, so this stays a local datastore read; at worst it coalesces with the fetch
     * the entry-point screen started.
     */
    suspend fun selectOfferToTake(
        value: OfferItemPresentationModel,
        takerProfileId: String?,
    ) {
        warmUpFirstTimeTraderFlag(takerProfileId)
        val isFirstTimeTrader =
            takerProfileId != null &&
                takerProfileId !in payoutAddressPrepRepository.fetch().profilesWithCompletedTrade
        selectOfferToTake(value, isFirstTimeTrader)
    }

    /** Sync overload without first-timer resolution: the address step stays out of the wizard. */
    fun selectOfferToTake(value: OfferItemPresentationModel) {
        selectOfferToTake(value, isFirstTimeTrader = false)
    }

    // Per-profile warm-up markers: an in-flight warm-up is awaited instead of duplicated, and a
    // finished one memoizes "resolved for this session" so re-entering the offerbook never
    // refetches. Main-thread confined like every other coordinator field ([takeOfferModel] etc.).
    private val firstTimeTraderWarmUps = mutableMapOf<String, CompletableDeferred<Unit>>()

    /**
     * Seeds the veteran flag from completed-trade history, so a user who traded before this
     * feature existed is never shown the first-timer address step. Launched in the background
     * from the entry-point screens as an optimization; the suspending [selectOfferToTake]
     * awaits it, which normally costs nothing because the fetch finished long before the tap.
     * Concurrent calls for the same profile coalesce into one fetch. Every profile on the
     * newest page of completed trades is marked; fresh completions are recorded live when a
     * trade reaches state 4. Both apps serve the same facade call (the node reads local
     * history, Connect the node's REST API). Fetch failures are logged and memoized for the
     * session: the flag stays local-only, and the worst case of a missed seed is one extra,
     * skippable wizard step.
     */
    suspend fun warmUpFirstTimeTraderFlag(selectedProfileId: String?) {
        if (selectedProfileId == null) return
        firstTimeTraderWarmUps[selectedProfileId]?.let {
            it.await()
            return
        }
        val marker = CompletableDeferred<Unit>()
        firstTimeTraderWarmUps[selectedProfileId] = marker
        try {
            seedFromCompletedTradeHistory(selectedProfileId)
        } catch (t: Throwable) {
            // Cancellation mid-fetch: release awaiters and forget the marker so a later
            // screen visit retries the seed.
            firstTimeTraderWarmUps.remove(selectedProfileId)
            marker.complete(Unit)
            throw t
        }
        marker.complete(Unit)
    }

    private suspend fun seedFromCompletedTradeHistory(selectedProfileId: String) {
        if (selectedProfileId in payoutAddressPrepRepository.fetch().profilesWithCompletedTrade) return
        val result =
            tradesServiceFacade.getClosedTradesPaginated(
                params = PaginationParams(page = PaginationParams.DEFAULT_PAGE, pageSize = PaginationParams.MAX_PAGE_SIZE),
                outcomeFilter = TradeOutcomeFilter.COMPLETED,
            )
        // The facades wrap failures in a Result, so a cancelled round trip surfaces as a
        // failure value — rethrow it instead of degrading cancellation to a logged miss
        // (same guard as checkTakeOfferEligibility).
        if (result.exceptionOrNull() is CancellationException) {
            currentCoroutineContext().ensureActive()
        }
        result
            .onSuccess { response ->
                response.items
                    .map { it.myUserProfile.id }
                    .toSet()
                    .forEach { profileId ->
                        runCatching { payoutAddressPrepRepository.markTradeCompleted(profileId) }
                            .onFailure {
                                if (it is CancellationException) throw it
                                log.w("Failed to seed completed-trade flag", it)
                            }
                    }
            }.onFailure { log.i { "Completed-trade history unavailable for first-timer seeding: $it" } }
    }

    private fun selectOfferToTake(
        value: OfferItemPresentationModel,
        isFirstTimeTrader: Boolean,
    ) {
        totalSteps = 1
        btcAddressStepCounted = false
        takeOfferModel = TakeOfferModel()
        takeOfferModel.offerItemPresentationVO = value
        takeOfferModel.isFirstTimeTrader = isFirstTimeTrader

        val offerListItem = takeOfferModel.offerItemPresentationVO
        val bisqEasyOffer = offerListItem.bisqEasyOffer

        takeOfferModel.hasMultipleQuoteSidePaymentMethods = bisqEasyOffer.quoteSidePaymentMethodSpecs.size > 1
        takeOfferModel.hasMultipleBaseSidePaymentMethods = bisqEasyOffer.baseSidePaymentMethodSpecs.size > 1

        val amountSpec = bisqEasyOffer.amountSpec

        val marketVO = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market
        val marketPriceItem: MarketPriceItem? = marketPriceServiceFacade.findMarketPriceItem(marketVO)
        takeOfferModel.originalPriceQuote = marketPriceItem?.priceQuote ?: getMostRecentPriceQuote()

        val priceQuote: PriceQuoteVO = getMostRecentPriceQuote()
        takeOfferModel.priceQuote = priceQuote

        val quoteCurrencyCode = bisqEasyOffer.market.quoteCurrencyCode
        val baseCurrencyCode = bisqEasyOffer.market.baseCurrencyCode
        var quoteAmount = FiatVOFactory.from(0, quoteCurrencyCode)
        var baseAmount = CoinVOFactory.from(0, baseCurrencyCode)

        // Determine if the offer truly has a selectable range after clamping with trade limits.
        // A RangeAmountSpec may collapse to a single value when the offer's range is narrower
        // than or equal to the trade amount limits. We compare after rounding to the slider step
        // (10,000 minor units) because the slider can only produce step-rounded values.
        var hasEffectiveRange = false
        if (amountSpec is RangeAmountSpecVO) {
            val sliderStep = 10_000L
            val limits = configServiceFacade.tradeAmountLimits.value
            val tradeLimitMin = BisqEasyTradeAmountLimits.getMinAmountValue(marketPriceServiceFacade, quoteCurrencyCode, limits)
            val tradeLimitMax = BisqEasyTradeAmountLimits.getMaxAmountValue(marketPriceServiceFacade, quoteCurrencyCode, limits)

            // If market price data is unavailable, getMin/MaxAmountValue return 0.
            // In that case, fall back to showing the amount screen and let
            // TakeOfferAmountPresenter handle the degraded state via its runCatching.
            if (tradeLimitMin > 0 && tradeLimitMax > 0) {
                val effectiveMin = maxOf(tradeLimitMin, amountSpec.minAmount)
                val effectiveMax = minOf(tradeLimitMax, amountSpec.maxAmount)
                hasEffectiveRange = effectiveMax > effectiveMin && (effectiveMax - effectiveMin) >= sliderStep
                if (!hasEffectiveRange && effectiveMin <= effectiveMax) {
                    // Range collapsed — treat as fixed amount using the midpoint
                    val fixedAmount = ((effectiveMin + effectiveMax) / 2).coerceIn(effectiveMin, effectiveMax)
                    quoteAmount = FiatVOFactory.from(fixedAmount, quoteCurrencyCode)
                    baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
                } else if (!hasEffectiveRange) {
                    // effectiveMin > effectiveMax: inverted range from bad data, show amount screen
                    hasEffectiveRange = true
                }
            } else {
                hasEffectiveRange = true
            }
        }
        takeOfferModel.hasAmountRange = hasEffectiveRange

        if (!takeOfferModel.hasAmountRange && amountSpec !is RangeAmountSpecVO) {
            if (amountSpec is QuoteSideFixedAmountSpecVO) {
                quoteAmount = FiatVOFactory.from(amountSpec.amount, quoteCurrencyCode)
                baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
            } else if (amountSpec is BaseSideFixedAmountSpecVO) {
                baseAmount = CoinVOFactory.from(amountSpec.amount, baseCurrencyCode)
                quoteAmount = priceQuote.toQuoteSideMonetary(baseAmount) as FiatVO
            }
        }
        if (takeOfferModel.hasAmountRange) {
            totalSteps = totalSteps + 1
        }
        takeOfferModel.quoteAmount = quoteAmount
        takeOfferModel.baseAmount = baseAmount

        var quoteSidePaymentMethod = ""
        if (!takeOfferModel.hasMultipleQuoteSidePaymentMethods) {
            quoteSidePaymentMethod = offerListItem.quoteSidePaymentMethods[0]
        } else {
            totalSteps = totalSteps + 1
        }
        takeOfferModel.quoteSidePaymentMethod = quoteSidePaymentMethod
        var baseSidePaymentMethod = ""
        if (!takeOfferModel.hasMultipleBaseSidePaymentMethods) {
            baseSidePaymentMethod = offerListItem.baseSidePaymentMethods[0]
        } else {
            totalSteps = totalSteps + 1
        }
        takeOfferModel.baseSidePaymentMethod = baseSidePaymentMethod

        // Single settlement method: the address step's presence is already final. With multiple
        // methods it stays out of the count until the user's choice commits to mainchain — the
        // one dynamic step in the wizard (see commitSettlementMethod).
        if (showBtcAddressScreen()) {
            totalSteps = totalSteps + 1
            btcAddressStepCounted = true
        }
    }

    fun showPaymentMethodsScreen(): Boolean = takeOfferModel.hasMultipleQuoteSidePaymentMethods

    fun showSettlementMethodsScreen(): Boolean = takeOfferModel.hasMultipleBaseSidePaymentMethods

    fun showAmountScreen(): Boolean = takeOfferModel.hasAmountRange

    /**
     * The optional payout-address step: first-time traders taking a SELL offer
     * (they receive the bitcoin) settled on mainchain. Lightning never collects early — an
     * invoice's amount and expiry make it unusable by the time the trade needs it. Before the
     * settlement choice commits on a multi-method offer this is false, so the step joins the
     * wizard only once mainchain is actually picked.
     */
    fun showBtcAddressScreen(): Boolean =
        takeOfferModel.isFirstTimeTrader &&
            takeOfferModel.takerIsBuyer &&
            takeOfferModel.baseSidePaymentMethod == BitcoinPaymentRailEnum.MAIN_CHAIN.name

    /**
     * The wizard step the flow starts on for the offer selected via [selectOfferToTake] — steps
     * that need no user input are skipped. Pure mapping: navigating there stays with the caller.
     */
    fun firstScreen(): NavRoute =
        when {
            showAmountScreen() -> NavRoute.TakeOfferTradeAmount
            showPaymentMethodsScreen() -> NavRoute.TakeOfferPaymentMethod
            showSettlementMethodsScreen() -> NavRoute.TakeOfferSettlementMethod
            // Only reachable as first screen when the settlement method is single (and mainchain),
            // so this branch is as stable across the flow's life as the ones above: with multiple
            // settlement methods the settlement branch already won.
            showBtcAddressScreen() -> NavRoute.TakeOfferBtcAddress
            else -> NavRoute.TakeOfferReviewTrade
        }

    /**
     * Reputation-based eligibility for taking [item], shared by every surface that can start the
     * take-offer flow (offerbook, peer profile) so the rule cannot drift between entry points.
     *
     * The seller's score is the one that must cover the offer amount:
     * - SELL offer → the maker sells, the MAKER's score is checked.
     * - BUY offer → the taker (this user) would sell, so [userProfile]'s own score is checked.
     *
     * A failed lookup counts as score 0 (strict policy; the "not cached yet" allowance is
     * deliberately disabled). Does not touch [takeOfferModel] — safe to call before
     * [selectOfferToTake].
     */
    suspend fun checkTakeOfferEligibility(
        item: OfferItemPresentationModel,
        userProfile: UserProfileVO,
    ): TakeOfferEligibility =
        withContext(computationDispatcher) {
            val bisqEasyOffer = item.bisqEasyOffer
            val limits = configServiceFacade.tradeAmountLimits.value
            val requiredReputationScoreForMaxOrFixed =
                BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(
                    marketPriceServiceFacade,
                    bisqEasyOffer,
                    limits,
                )
            require(requiredReputationScoreForMaxOrFixed != null) { "requiredReputationScoreForMaxOrFixedAmount is null" }
            val requiredReputationScoreForMinOrFixed =
                BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(
                    marketPriceServiceFacade,
                    bisqEasyOffer,
                    limits,
                )
            require(requiredReputationScoreForMinOrFixed != null) { "requiredReputationScoreForMinAmount is null" }

            val market = bisqEasyOffer.market
            val quoteCurrencyCode = market.quoteCurrencyCode
            val minFiatAmount: String =
                AmountFormatter.formatAmount(
                    FiatVOFactory.from(bisqEasyOffer.getFixedOrMinAmount(), quoteCurrencyCode),
                    useLowPrecision = true,
                    withCode = true,
                )
            val maxFiatAmount: String =
                AmountFormatter.formatAmount(
                    FiatVOFactory.from(bisqEasyOffer.getFixedOrMaxAmount(), quoteCurrencyCode),
                    useLowPrecision = true,
                    withCode = true,
                )

            // For BUY offers: The maker wants to buy Bitcoin, so the taker (me) becomes the seller
            // For SELL offers: The maker wants to sell Bitcoin, so the maker becomes the seller
            val userProfileId =
                if (bisqEasyOffer.direction == DirectionEnum.SELL) {
                    bisqEasyOffer.makerNetworkId.pubKey.id // Offer maker is seller (wants to sell Bitcoin)
                } else {
                    userProfile.id // I am seller (taker selling to maker who wants to buy)
                }

            val reputationResult: Result<ReputationScoreVO> = reputationServiceFacade.getReputation(userProfileId)

            // The facades wrap failures in a Result, so a cancelled round trip surfaces as a
            // failure value — rethrow it instead of degrading cancellation to "score 0"
            // (same guard as OfferbookPresenter.getMyReputation / BisqEasyTradeAmountLimits).
            if (reputationResult.exceptionOrNull() is CancellationException) {
                currentCoroutineContext().ensureActive()
            }

            val sellersScore: Long = reputationResult.getOrNull()?.totalScore ?: 0
            val isReputationNotCached = reputationResult.exceptionOrNull()?.message?.contains("not cached yet") == true

            reputationResult.exceptionOrNull()?.let { exception ->
                log.w("Exception at reputationServiceFacade.getReputation", exception)
                if (isReputationNotCached) {
                    log.i { "Reputation not cached yet for the checked seller, allowing offer to be taken" }
                }
            }

            val isAmountRangeOffer = bisqEasyOffer.amountSpec is RangeAmountSpecVO

            // val canBuyerTakeOffer = isReputationNotCached || sellersScore >= requiredReputationScoreForMinOrFixed
            val canBuyerTakeOffer = sellersScore >= requiredReputationScoreForMinOrFixed
            if (canBuyerTakeOffer) {
                TakeOfferEligibility.Eligible
            } else {
                val link = "hyperlinks.openInBrowser.attention".i18n(BisqLinks.REPUTATION_WIKI_URL)
                val takersDirection = bisqEasyOffer.direction.mirror
                val isSellerAsTakerWarning = takersDirection == DirectionEnum.SELL
                val headline: String
                val message: String
                if (takersDirection == DirectionEnum.BUY) {
                    // SELL offer: Maker wants to sell Bitcoin, so they are the seller
                    // Taker (me) wants to buy Bitcoin - checking if seller has enough reputation
                    val learnMore = "mobile.reputation.learnMoreAtWiki".i18n()
                    headline = "chat.message.takeOffer.buyer.invalidOffer.headline".i18n()
                    val warningKey =
                        if (isAmountRangeOffer) {
                            "chat.message.takeOffer.buyer.invalidOffer.rangeAmount.text"
                        } else {
                            "chat.message.takeOffer.buyer.invalidOffer.fixedAmount.text"
                        }

                    message = warningKey.i18n(
                        sellersScore,
                        if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                        if (isAmountRangeOffer) minFiatAmount else maxFiatAmount,
                    ) + "\n\n" + learnMore + "\n\n" + link
                } else {
                    // BUY offer: Maker wants to buy Bitcoin, so taker becomes the seller
                    // Taker (me) wants to sell Bitcoin - checking if I have enough reputation
                    headline = "chat.message.takeOffer.seller.insufficientScore.headline".i18n()
                    val warningKey =
                        if (isAmountRangeOffer) {
                            "chat.message.takeOffer.seller.insufficientScore.rangeAmount.warning"
                        } else {
                            "chat.message.takeOffer.seller.insufficientScore.fixedAmount.warning"
                        }
                    message = warningKey.i18n(
                        sellersScore,
                        if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                        if (isAmountRangeOffer) minFiatAmount else maxFiatAmount,
                    ) + "\n\n" + "mobile.reputation.warning.navigateToReputation".i18n()
                }
                TakeOfferEligibility.NotEnoughReputation(headline, message, isSellerAsTakerWarning)
            }
        }

    fun commitAmount(
        priceQuote: PriceQuoteVO,
        quoteAmount: FiatVO,
        baseAmount: CoinVO,
    ) {
        takeOfferModel.priceQuote = priceQuote
        takeOfferModel.quoteAmount = quoteAmount
        takeOfferModel.baseAmount = baseAmount
    }

    fun commitPaymentMethod(quoteSidePaymentMethod: String) {
        takeOfferModel.quoteSidePaymentMethod = quoteSidePaymentMethod
    }

    fun commitSettlementMethod(baseSidePaymentMethod: String) {
        takeOfferModel.baseSidePaymentMethod = baseSidePaymentMethod
        // The address step is the wizard's one dynamic step: on a multi-method offer its presence
        // is only known once the settlement choice commits. Keep totalSteps and any collected
        // address consistent with the current choice — a back-navigation that re-picks Lightning
        // must also drop a mainchain address collected en route.
        val shows = showBtcAddressScreen()
        if (shows && !btcAddressStepCounted) {
            totalSteps = totalSteps + 1
            btcAddressStepCounted = true
        } else if (!shows && btcAddressStepCounted) {
            totalSteps = totalSteps - 1
            btcAddressStepCounted = false
            takeOfferModel.btcAddress = ""
        }
    }

    fun commitBtcAddress(address: String) {
        takeOfferModel.btcAddress = address.trim()
    }

    suspend fun takeOffer(): TakeOfferFlowResult {
        val takeOfferStatus = MutableStateFlow<TakeOfferStatus?>(null)
        val takeOfferErrorMessage = MutableStateFlow<String?>(null)

        val result =
            tradesServiceFacade.takeOffer(
                takeOfferModel.offerItemPresentationVO.bisqEasyOffer,
                takeOfferModel.baseAmount,
                takeOfferModel.quoteAmount,
                takeOfferModel.baseSidePaymentMethod,
                takeOfferModel.quoteSidePaymentMethod,
                takeOfferStatus,
                takeOfferErrorMessage,
            )
        if (result.isSuccess) {
            val tradeId = result.getOrThrow()
            tradesServiceFacade.selectOpenTrade(tradeId)
            // Hand the wizard-collected payout address to the mid-trade address screen. Persisted
            // (not in-memory) because that screen can be reached after an app restart. Best-effort:
            // a failed write only costs the pre-fill, never the trade.
            if (takeOfferModel.btcAddress.isNotBlank()) {
                runCatching { payoutAddressPrepRepository.setPrefill(tradeId, takeOfferModel.btcAddress) }
                    .onFailure {
                        if (it is CancellationException) throw it
                        log.w("Failed to persist payout-address prefill", it)
                    }
            }
        } else {
            log.w { "Take offer failed ${result.exceptionOrNull()}" }
            // Safety net: the facades are expected to populate takeOfferErrorMessage on failure,
            // but if one returned a bare failure the presenter would keep the progress dialog up
            // forever waiting for an emission that never comes.
            if (takeOfferErrorMessage.value == null) {
                takeOfferErrorMessage.value =
                    result.exceptionOrNull()?.let { ExpectedTradeProtocolRejection.fromThrowable(it) }
                        ?: "mobile.takeOffer.unexpectedError".i18n()
            }
        }
        return TakeOfferFlowResult(takeOfferStatus, takeOfferErrorMessage)
    }

    fun getMostRecentPriceQuote(): PriceQuoteVO {
        val marketVO = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market
        val marketPriceItem: MarketPriceItem? = marketPriceServiceFacade.findMarketPriceItem(marketVO)
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        if (marketPriceItem != null) {
            return priceSpec.getPriceQuoteVO(marketPriceItem)
        } else {
            // FIXME happens in client mode. probably market price data are not received in time
            log.e { "marketPriceItem must not be null" }
            val item: MarketPriceItem? = marketPriceServiceFacade.findMarketPriceItem(marketVO)
            return PriceQuoteVO(
                0,
                4,
                2,
                marketVO,
                CoinVOFactory.bitcoinFrom(1),
                FiatVOFactory.from(
                    item?.priceQuote?.value ?: 0L,
                    item?.market?.quoteCurrencyCode ?: "USD",
                ),
            )
        }
    }
}
