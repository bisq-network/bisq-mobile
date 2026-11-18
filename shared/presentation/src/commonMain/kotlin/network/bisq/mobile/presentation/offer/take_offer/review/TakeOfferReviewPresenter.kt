package network.bisq.mobile.presentation.offer.take_offer.review

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.account.payment_method.BitcoinPaymentRailEnum
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.displayString
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.data.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.service.trades.ExpectedTradeProtocolRejection
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsDisplayMode
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsUiState
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.i18NPaymentMethod
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.OfferFlowPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewUiState.AddressNotice
import kotlin.math.abs

class TakeOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val takeOfferCoordinator: TakeOfferCoordinator,
    communityHubService: CommunityHubService,
) : OfferFlowPresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.TakeOfferReview

    private val takeOfferModel: TakeOfferCoordinator.TakeOfferModel = takeOfferCoordinator.takeOfferModel

    // Same predicate as Help / open-trade failure surfaces: DISCUSSIONS live means the
    // Support channel can be filled. Seeded from the Eager liveSegments so the Trade
    // Failed dialog does not pop the button in a frame late.
    private val _uiState =
        MutableStateFlow(
            buildInitialUiState(
                isSupportChannelAvailable = CommunitySegment.DISCUSSIONS in communityHubService.liveSegments.value,
            ),
        )
    val uiState: StateFlow<TakeOfferReviewUiState> = _uiState.asStateFlow()

    // We pass that to the domain, which updates the state while take offer is in progress, so that we can show the status
    // or error to the user
    private val takeOfferStatus: MutableStateFlow<TakeOfferStatus?> = MutableStateFlow(null)
    private val takeOfferErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    // Atomic guard against rapid-fire taps on the "Take offer" button. The progress
    // dialog is the visible signal, but its modality is not enough on its own —
    // touches can land before the dialog renders, especially on the android node
    // app where the API returns near-instantly. compareAndSet ensures only the
    // first tap proceeds; subsequent taps short-circuit until SUCCESS / error.
    private val isTakingOffer = MutableStateFlow(false)

    init {
        presenterScope.launch {
            communityHubService.liveSegments
                .map { CommunitySegment.DISCUSSIONS in it }
                .collect { available -> _uiState.update { it.copy(isSupportChannelAvailable = available) } }
        }
        presenterScope.launch {
            takeOfferStatus.collect {
                log.i { "takeOfferStatus: $it" }
                // sendTakeOfferMessage can report SUCCESS before a maker protocol
                // rejection lands on the error flow. Error wins — do not cover
                // Trade Failed with "You have successfully taken the offer".
                if (it == TakeOfferStatus.SUCCESS && takeOfferErrorMessage.value == null) {
                    // Keep isTakingOffer = true on success: the user moves on via
                    // the success dialog. Resetting could re-expose the button if
                    // the dialog were dismissed unexpectedly.
                    _uiState.update { state ->
                        state.copy(showTakeOfferSuccessDialog = true, showTakeOfferProgressDialog = false)
                    }
                }
            }
        }
        presenterScope.launch {
            // Nulls are the initial state and the per-attempt reset in onTakeOffer; only real
            // errors matter. The reset also re-arms the StateFlow so a retry failing with the
            // exact same message still emits (value dedup would otherwise swallow it and leave
            // the progress dialog up).
            takeOfferErrorMessage.filterNotNull().collect { message ->
                log.e { "takeOfferErrorMessage: $message" }
                val expected = ExpectedTradeProtocolRejection.extractExpected(message)
                val errorDialog =
                    if (expected != null) {
                        TakeOfferErrorDialog.ProtocolFailure(
                            expected,
                            atPeer = ExpectedTradeProtocolRejection.isAtPeer(message),
                        )
                    } else {
                        TakeOfferErrorDialog.Unexpected(message)
                    }
                // Error path: hide progress and any premature success dialog, and
                // release the guard so the user can retry.
                _uiState.update {
                    it.copy(
                        takeOfferErrorDialog = errorDialog,
                        showTakeOfferSuccessDialog = false,
                        showTakeOfferProgressDialog = false,
                    )
                }
                isTakingOffer.value = false
            }
        }
    }

    fun onAction(action: TakeOfferReviewUiAction) {
        when (action) {
            TakeOfferReviewUiAction.OnBack -> navigateBack()
            TakeOfferReviewUiAction.OnClose ->
                // The X abandons the whole flow: land exactly one step below the wizard's entry —
                // the offerbook, the peer profile, or the peer-offers screen, whichever launched it.
                // firstScreen() is stable for the life of the flow (the step flags never change
                // after selectOfferToTake), so it identifies the wizard's first screen at any step.
                navigateBackTo(takeOfferCoordinator.firstScreen(), shouldInclusive = true)
            TakeOfferReviewUiAction.OnTakeOffer -> onTakeOffer()
            TakeOfferReviewUiAction.OnGoToOpenTrades -> {
                _uiState.update { it.copy(showTakeOfferSuccessDialog = false) }
                navigateToTab(NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN))
            }
            TakeOfferReviewUiAction.OnDismissTakeOfferError -> _uiState.update { it.copy(takeOfferErrorDialog = null) }
            TakeOfferReviewUiAction.OnOpenSupportChannel -> navigateTo(NavRoute.SupportChannel)
            // The address step immediately precedes review whenever an address was collected.
            TakeOfferReviewUiAction.OnEditAddress -> navigateBack()
            TakeOfferReviewUiAction.OnOpenWalletGuide -> navigateTo(NavRoute.WalletGuideIntro)
            TakeOfferReviewUiAction.OnToggleTradeTerms ->
                _uiState.update { state ->
                    state.copy(tradeTerms = state.tradeTerms?.let { it.copy(isExpanded = !it.isExpanded) })
                }
        }
    }

    private fun onTakeOffer() {
        if (isDemo()) {
            showSnackbar("mobile.demo.action.disabled".i18n(), type = SnackbarType.ERROR)
            return
        }
        if (!isTakingOffer.compareAndSet(expect = false, update = true)) {
            log.w { "onTakeOffer called while a take is already in progress; ignoring" }
            return
        }
        _uiState.update {
            it.copy(
                showTakeOfferProgressDialog = true,
                showTakeOfferSuccessDialog = false,
                takeOfferErrorDialog = null,
            )
        }
        // Reset per-attempt state so a repeat of the previous outcome still emits.
        takeOfferStatus.value = null
        takeOfferErrorMessage.value = null
        presenterScope.launch {
            try {
                val (statusFlow, errorFlow) = takeOfferCoordinator.takeOffer()

                // The stateFlow objects are set in the ioScope in the service. Thus we need to map them to the presenterScope.
                presenterScope.launch {
                    statusFlow.collect { takeOfferStatus.value = it }
                }
                presenterScope.launch {
                    errorFlow.collect { takeOfferErrorMessage.value = it }
                }
            } catch (e: Exception) {
                // Job cancellation must propagate. TimeoutCancellationException is also a
                // CancellationException — keep that path so fromThrowable can map it to
                // sendTimedOut instead of dropping the user-facing timeout copy.
                if (e is CancellationException && !ExpectedTradeProtocolRejection.isTimeout(e)) {
                    _uiState.update { it.copy(showTakeOfferProgressDialog = false) }
                    isTakingOffer.value = false
                    throw e
                }
                log.e("Take offer failed", e)
                takeOfferErrorMessage.value = ExpectedTradeProtocolRejection.fromThrowable(e)
                _uiState.update { it.copy(showTakeOfferProgressDialog = false) }
                isTakingOffer.value = false
            }
        }
    }

    private fun buildInitialUiState(isSupportChannelAvailable: Boolean): TakeOfferReviewUiState {
        val offer = takeOfferModel.offerItemPresentationVO.bisqEasyOffer
        val takersDirection = offer.direction.mirror
        val formattedQuoteAmount = AmountFormatter.formatAmount(takeOfferModel.quoteAmount, true, true)
        val formattedBaseAmount = AmountFormatter.formatAmount(takeOfferModel.baseAmount, false, false)
        val isBuyer = takersDirection.isBuy

        return TakeOfferReviewUiState(
            headLine = "${takersDirection.displayString.uppercase()} Bitcoin",
            takersDirection = takersDirection,
            amountToPay = if (isBuyer) formattedQuoteAmount else formattedBaseAmount,
            amountToReceive = if (isBuyer) formattedBaseAmount else formattedQuoteAmount,
            price = PriceQuoteFormatter.format(takeOfferModel.priceQuote, true, false),
            marketCodes = offer.market.marketCodes,
            priceDetails = resolvePriceDetails(),
            quoteSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.quoteSidePaymentMethod).first,
            baseSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.baseSidePaymentMethod).first,
            fee =
                if (isBuyer) {
                    "bisqEasy.tradeWizard.review.noTradeFees".i18n()
                } else {
                    "bisqEasy.tradeWizard.review.sellerPaysMinerFee".i18n()
                },
            feeDetails =
                if (isBuyer) {
                    "bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong".i18n()
                } else {
                    "bisqEasy.tradeWizard.review.noTradeFeesLong".i18n()
                },
            addressNotice = resolveAddressNotice(takersDirection),
            // The terms attached to the offer, not the maker's current profile terms: these are what
            // the taker agrees to, even if the maker has edited their profile since.
            tradeTerms =
                TradeTermsUiState.fromText(
                    offer.offerOptions
                        .filterIsInstance<TradeTermsOptionVO>()
                        .firstOrNull()
                        ?.makersTradeTerms,
                    TradeTermsDisplayMode.COLLAPSIBLE,
                    maxLength = MAX_REVIEW_TRADE_TERMS_LENGTH,
                ),
            isSupportChannelAvailable = isSupportChannelAvailable,
        )
    }

    private fun resolveAddressNotice(takersDirection: DirectionEnum): AddressNotice? {
        if (!takersDirection.isBuy) return null
        return when {
            takeOfferModel.baseSidePaymentMethod == BitcoinPaymentRailEnum.LN.name -> AddressNotice.Lightning
            takeOfferModel.btcAddress.isNotBlank() ->
                AddressNotice.MainchainConfirmed(truncateAddress(takeOfferModel.btcAddress))
            // Mainchain veterans get nothing: they know the drill, and by construction they never
            // saw the early-entry step either.
            takeOfferModel.isFirstTimeTrader -> AddressNotice.MainchainAnnounce
            else -> null
        }
    }

    private fun resolvePriceDetails(): String {
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        val percent =
            PriceUtil.findPercentFromMarketPrice(
                marketPriceServiceFacade,
                priceSpec,
                takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market,
            )
        if ((priceSpec is FloatPriceSpecVO || priceSpec is MarketPriceSpecVO) && percent == 0.0) {
            return "bisqEasy.tradeWizard.review.priceDetails".i18n()
        }
        val priceWithCode = PriceQuoteFormatter.format(takeOfferModel.originalPriceQuote, true, true)
        val percentagePrice = PercentageFormatter.format(abs(percent), true)
        val aboveOrBelow: String = if (percent > 0) "mobile.general.above".i18n() else "mobile.general.below".i18n()
        return if (priceSpec is FloatPriceSpecVO) {
            "bisqEasy.tradeWizard.review.priceDetails.float".i18n(percentagePrice, aboveOrBelow, priceWithCode)
        } else if (percent == 0.0) {
            "bisqEasy.tradeWizard.review.priceDetails.fix.atMarket".i18n(priceWithCode)
        } else {
            "bisqEasy.tradeWizard.review.priceDetails.fix".i18n(percentagePrice, aboveOrBelow, priceWithCode)
        }
    }

    private companion object {
        /**
         * bisq2 accepts up to 10,000 characters of terms on an offer, against 500 on a profile. The
         * review step is a decision screen, so a hostile maker must not be able to make it
         * arbitrarily long.
         */
        const val MAX_REVIEW_TRADE_TERMS_LENGTH = 2_000

        /** "bc1qw508d6…f3t4" — enough of both ends to eyeball, never a full-width line. */
        fun truncateAddress(address: String): String = if (address.length <= 16) address else "${address.take(10)}…${address.takeLast(4)}"
    }
}
