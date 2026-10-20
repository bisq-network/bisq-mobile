package network.bisq.mobile.presentation.offer.take_offer.amount

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.faceValueToLong
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.utils.getGroupingSeparator
import network.bisq.mobile.data.utils.toDoubleOrNullLocaleAware
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.MonetarySlider
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.OfferFlowPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator

// TODO Create/Take offer amount preseenters are very similar a base class could be extracted
class TakeOfferAmountPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val takeOfferCoordinator: TakeOfferCoordinator,
    private val configServiceFacade: ConfigServiceFacade,
) : OfferFlowPresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.TakeOfferAmount

    private val _uiState = MutableStateFlow(TakeOfferAmountUiState())
    val uiState: StateFlow<TakeOfferAmountUiState> = _uiState.asStateFlow()

    // Guard to prevent interactions when initialization fails
    private var initializationFailed: Boolean = false

    private lateinit var takeOfferModel: TakeOfferCoordinator.TakeOfferModel
    private val tradeAmountLimits get() = configServiceFacade.tradeAmountLimits.value

    private var minAmount: Long = 0L
    private var maxAmount: Long = 0L

    private lateinit var priceQuote: PriceQuoteVO
    private lateinit var quoteAmount: FiatVO
    private lateinit var baseAmount: CoinVO

    private var dragUpdateJob: Job? = null
    private var latestPending: Float? = null

    // Sample heavy updates during drags to reduce allocation churn on main thread.
    // 32ms ~ 30 FPS. Rationale: keep UI responsive and informative while limiting GC pressure.
    private val dragUpdateSampleMs: Long = 32

    init {
        runCatching {
            takeOfferModel = takeOfferCoordinator.takeOfferModel
            val offerListItem = takeOfferModel.offerItemPresentationVO
            val quoteCurrencyCode = offerListItem.bisqEasyOffer.market.quoteCurrencyCode

            val rangeAmountSpec: RangeAmountSpecVO =
                offerListItem.bisqEasyOffer.amountSpec as RangeAmountSpecVO

            minAmount = BisqEasyTradeAmountLimits.getMinAmountValue(marketPriceServiceFacade, quoteCurrencyCode, tradeAmountLimits)
            maxAmount = BisqEasyTradeAmountLimits.getMaxAmountValue(marketPriceServiceFacade, quoteCurrencyCode, tradeAmountLimits)

            minAmount = maxOf(minAmount, rangeAmountSpec.minAmount)
            maxAmount = minOf(maxAmount, rangeAmountSpec.maxAmount)

            val valueInFraction =
                if (takeOfferModel.quoteAmount.value == 0L) {
                    0.5F
                } else {
                    MonetarySlider.minorToFraction(takeOfferModel.quoteAmount.value, minAmount, maxAmount)
                }
            _uiState.value =
                TakeOfferAmountUiState(
                    quoteCurrencyCode = quoteCurrencyCode,
                    formattedMinAmount = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode)),
                    formattedMinAmountWithCode = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode), true, true),
                    formattedMaxAmountWithCode = AmountFormatter.formatAmount(FiatVOFactory.from(maxAmount, quoteCurrencyCode), true, true),
                    formattedQuoteAmount = offerListItem.formattedQuoteAmount,
                    formattedBaseAmount = offerListItem.formattedBaseAmount.value,
                    sliderPosition = valueInFraction,
                )
            applySliderValue(valueInFraction)
        }.onFailure { e ->
            log.e(e) { "Failed to init take offer data" }
            // Mark initialization failure and put UI in safe state
            initializationFailed = true
            _uiState.value = TakeOfferAmountUiState(amountValid = false)
        }
    }

    fun onAction(action: TakeOfferAmountUiAction) {
        when (action) {
            is TakeOfferAmountUiAction.OnSliderValueChange -> onSliderValueChanged(action.position)
            TakeOfferAmountUiAction.OnSliderDragFinished -> onSliderDragFinished()
            is TakeOfferAmountUiAction.OnTextValueChange -> onTextValueChanged(action.text)
            TakeOfferAmountUiAction.OnNext -> onNext()
            TakeOfferAmountUiAction.OnBack -> onBack()
            TakeOfferAmountUiAction.OnClose -> onClose()
        }
    }

    private fun onSliderValueChanged(sliderPosition: Float) {
        _uiState.update { it.copy(amountValid = sliderPosition in 0f..1f, sliderPosition = sliderPosition) }

        if (dragUpdateJob == null) {
            // Leading-edge immediate update for responsive feedback
            applySliderValue(sliderPosition)
            dragUpdateJob =
                presenterScope.launch {
                    delay(dragUpdateSampleMs)
                    latestPending?.let {
                        applySliderValue(it)
                        latestPending = null
                    }
                    dragUpdateJob = null
                }
        } else {
            // Coalesce subsequent updates within the sample window
            latestPending = sliderPosition
        }
    }

    private fun onSliderDragFinished() {
        dragUpdateJob?.cancel()
        latestPending?.let { applySliderValue(it) }
        latestPending = null
        dragUpdateJob = null
    }

    private fun onTextValueChanged(textInput: String) {
        if (initializationFailed) {
            _uiState.update { it.copy(formattedQuoteAmount = "", amountValid = false) }
            return
        }
        runCatching {
            val separator = getGroupingSeparator().toString()
            val value = textInput.toDoubleOrNullLocaleAware()
            if (value != null) {
                val exactMinor = FiatVOFactory.faceValueToLong(value)
                quoteAmount = FiatVOFactory.from(exactMinor, _uiState.value.quoteCurrencyCode)
                priceQuote = takeOfferCoordinator.getMostRecentPriceQuote()
                baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
                val clampedForSlider = exactMinor.coerceIn(minAmount, maxAmount)
                _uiState.update {
                    it.copy(
                        amountValid = exactMinor in minAmount..maxAmount,
                        formattedQuoteAmount = AmountFormatter.formatAmount(quoteAmount).replace(separator, ""),
                        formattedBaseAmount = AmountFormatter.formatAmount(baseAmount, false),
                        sliderPosition = MonetarySlider.minorToFraction(clampedForSlider, minAmount, maxAmount),
                    )
                }
            } else {
                _uiState.update { it.copy(formattedQuoteAmount = "", amountValid = false) }
            }
        }.onFailure { e ->
            log.e(e) { "Failed to handle text value change on take offer" }
            _uiState.update { it.copy(amountValid = false) }
        }
    }

    private fun onBack() {
        commitToModel()
        navigateBack()
    }

    private fun onClose() {
        // The X abandons the whole flow: land exactly one step below the wizard's entry —
        // the offerbook, the peer profile, or the peer-offers screen, whichever launched it.
        // firstScreen() is stable for the life of the flow (the step flags never change
        // after selectOfferToTake), so it identifies the wizard's first screen at any step.
        navigateBackTo(takeOfferCoordinator.firstScreen(), shouldInclusive = true)
    }

    private fun onNext() {
        commitToModel()

        if (takeOfferCoordinator.showPaymentMethodsScreen()) {
            navigateTo(NavRoute.TakeOfferPaymentMethod)
        } else if (takeOfferCoordinator.showSettlementMethodsScreen()) {
            navigateTo(NavRoute.TakeOfferSettlementMethod)
        } else if (takeOfferCoordinator.showBtcAddressScreen()) {
            navigateTo(NavRoute.TakeOfferBtcAddress)
        } else {
            navigateTo(NavRoute.TakeOfferReviewTrade)
        }
    }

    private fun applySliderValue(sliderPosition: Float) {
        if (initializationFailed) {
            _uiState.update { it.copy(amountValid = false) }
            return
        }
        try {
            val separator = getGroupingSeparator().toString()
            _uiState.update { it.copy(amountValid = true, sliderPosition = sliderPosition) }
            val roundedFiatValue: Long = MonetarySlider.fractionToAmountLong(sliderPosition, minAmount, maxAmount, 10_000L)
            quoteAmount = FiatVOFactory.from(roundedFiatValue, _uiState.value.quoteCurrencyCode)
            _uiState.update { it.copy(formattedQuoteAmount = AmountFormatter.formatAmount(quoteAmount).replace(separator, "")) }

            priceQuote = takeOfferCoordinator.getMostRecentPriceQuote()
            baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
            _uiState.update { it.copy(formattedBaseAmount = AmountFormatter.formatAmount(baseAmount, false)) }
        } catch (e: Exception) {
            log.e(e) { "Failed to apply slider value on take offer" }
        }
    }

    private fun commitToModel() {
        if (initializationFailed) return
        takeOfferCoordinator.commitAmount(priceQuote, quoteAmount, baseAmount)
    }
}
