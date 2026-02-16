package network.bisq.mobile.presentation.offer.take_offer.amount

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.faceValueToLong
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.getDecimalSeparator
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.MonetarySlider
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.AmountValidator
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferPresenter

// TODO Create/Take offer amount preseenters are very similar a base class could be extracted
class TakeOfferAmountPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
) : BasePresenter(mainPresenter) {
    private val _sliderPosition: MutableStateFlow<Float> = MutableStateFlow(0.5f)
    val sliderPosition: StateFlow<Float> get() = _sliderPosition.asStateFlow()

    var quoteCurrencyCode: String = ""
    var formattedMinAmount: String = ""
    var formattedMinAmountWithCode: String = ""
    var formattedMaxAmountWithCode: String = ""
    private val _formattedQuoteAmount = MutableStateFlow("")
    val formattedQuoteAmount: StateFlow<String> get() = _formattedQuoteAmount.asStateFlow()
    private val _formattedBaseAmount = MutableStateFlow("")
    val formattedBaseAmount: StateFlow<String> get() = _formattedBaseAmount.asStateFlow()

    // Guard to prevent interactions when initialization fails
    private var initializationFailed: Boolean = false

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel
    private var minAmount: Long = 0L
    private var maxAmount: Long = 0L

    private lateinit var priceQuote: PriceQuoteVO
    private lateinit var quoteAmount: FiatVO
    private lateinit var baseAmount: CoinVO

    private val _amountValid: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val amountValid: StateFlow<Boolean> get() = _amountValid.asStateFlow()

    private var dragUpdateJob: Job? = null

    // Sample heavy updates during drags to reduce allocation churn on main thread.
    // 32ms ~ 30 FPS. Rationale: keep UI responsive and informative while limiting GC pressure.
    private val dragUpdateSampleMs: Long = 32

    init {
        runCatching {
            takeOfferModel = takeOfferPresenter.takeOfferModel
            val offerListItem = takeOfferModel.offerItemPresentationVO
            quoteCurrencyCode = offerListItem.bisqEasyOffer.market.quoteCurrencyCode

            val rangeAmountSpec: RangeAmountSpecVO =
                offerListItem.bisqEasyOffer.amountSpec as RangeAmountSpecVO

            minAmount = BisqEasyTradeAmountLimits.getMinAmountValue(marketPriceServiceFacade, quoteCurrencyCode)
            maxAmount = BisqEasyTradeAmountLimits.getMaxAmountValue(marketPriceServiceFacade, quoteCurrencyCode)

            minAmount = maxOf(minAmount, rangeAmountSpec.minAmount)
            maxAmount = minOf(maxAmount, rangeAmountSpec.maxAmount)

            formattedMinAmount = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode))
            formattedMinAmountWithCode =
                AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode), true, true)
            formattedMaxAmountWithCode =
                AmountFormatter.formatAmount(FiatVOFactory.from(maxAmount, quoteCurrencyCode), true, true)

            _formattedQuoteAmount.value = offerListItem.formattedQuoteAmount
            _formattedBaseAmount.value = offerListItem.formattedBaseAmount.value

            val valueInFraction =
                if (takeOfferModel.quoteAmount.value == 0L) {
                    0.5F
                } else {
                    MonetarySlider.minorToFraction(takeOfferModel.quoteAmount.value, minAmount, maxAmount)
                }
            _sliderPosition.value = valueInFraction
            applySliderValue(sliderPosition.value)
        }.onFailure { e ->
            log.e(e) { "Failed to init take offer data" }
            // Mark initialization failure and put UI in safe state
            initializationFailed = true
            quoteCurrencyCode = ""
            formattedMinAmount = ""
            formattedMinAmountWithCode = ""
            formattedMaxAmountWithCode = ""
            _formattedQuoteAmount.value = ""
            _formattedBaseAmount.value = ""
            _amountValid.value = false
        }
    }

    fun onSliderValueChanged(sliderPosition: Float) {
        _amountValid.value = sliderPosition in 0f..1f
        _sliderPosition.value = sliderPosition

        dragUpdateJob?.cancel()
        var job: Job? = null
        job =
            presenterScope.launch {
                delay(dragUpdateSampleMs)
                applySliderValue(this@TakeOfferAmountPresenter.sliderPosition.value, trackedJob = job)
            }
        dragUpdateJob = job
    }

    fun onSliderDragFinished() {
        dragUpdateJob?.cancel()
        dragUpdateJob = null
        applySliderValue(sliderPosition.value)
    }

    fun onTextValueChanged(textInput: String) {
        if (initializationFailed) {
            _formattedQuoteAmount.value = ""
            _amountValid.value = false
            return
        }
        runCatching {
            val _value = textInput.toDoubleOrNullLocaleAware()
            if (_value != null) {
                val exactMinor = FiatVOFactory.faceValueToLong(_value)
                val isInRange = exactMinor in minAmount..maxAmount
                _amountValid.value = isInRange
                quoteAmount = FiatVOFactory.from(exactMinor, quoteCurrencyCode)
                _formattedQuoteAmount.value = AmountFormatter.formatAmount(quoteAmount)
                priceQuote = takeOfferPresenter.getMostRecentPriceQuote()
                baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
                _formattedBaseAmount.value = AmountFormatter.formatAmount(baseAmount, false)
                val clampedForSlider = exactMinor.coerceIn(minAmount, maxAmount)
                _sliderPosition.value = MonetarySlider.minorToFraction(clampedForSlider, minAmount, maxAmount)
            } else {
                _formattedQuoteAmount.value = ""
                _amountValid.value = false
            }
        }.onFailure { e ->
            log.e(e) { "Failed to handle text value change on take offer" }
            _amountValid.value = false
        }
    }

    fun validateTextField(value: String): String? = AmountValidator.validate(value, minAmount, maxAmount)

    fun getFractionForFiat(value: Double): Float {
        val range = (maxAmount - minAmount).takeIf { it != 0L } ?: return 0f
        val inFraction = ((value * 10000) - minAmount) / range
        return inFraction.toFloat()
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onClose() {
        navigateToOfferbookTab()
    }

    fun onNext() {
        commitToModel()

        if (takeOfferPresenter.showPaymentMethodsScreen()) {
            navigateTo(NavRoute.TakeOfferPaymentMethod)
        } else if (takeOfferPresenter.showSettlementMethodsScreen()) {
            navigateTo(NavRoute.TakeOfferSettlementMethod)
        } else {
            navigateTo(NavRoute.TakeOfferReviewTrade)
        }
    }

    private fun applySliderValue(
        sliderPosition: Float,
        trackedJob: Job? = null,
    ) {
        if (initializationFailed) {
            _amountValid.value = false
            if (trackedJob != null && dragUpdateJob === trackedJob) {
                dragUpdateJob = null
            }
            return
        }
        try {
            _sliderPosition.value = sliderPosition
            val roundedFiatValue: Long = MonetarySlider.fractionToAmountLong(sliderPosition, minAmount, maxAmount, 10_000L)
            quoteAmount = FiatVOFactory.from(roundedFiatValue, quoteCurrencyCode)
            _formattedQuoteAmount.value = AmountFormatter.formatAmount(quoteAmount)

            val decimalSeparator = getDecimalSeparator()
            val formattedFiatAmountValueString = _formattedQuoteAmount.value.substringBefore(decimalSeparator)

            _amountValid.value = validateTextField(formattedFiatAmountValueString) == null

            priceQuote = takeOfferPresenter.getMostRecentPriceQuote()
            baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
            _formattedBaseAmount.value = AmountFormatter.formatAmount(baseAmount, false)
        } catch (e: Exception) {
            log.e(e) { "Failed to apply slider value on take offer" }
        } finally {
            if (trackedJob != null && dragUpdateJob === trackedJob) {
                dragUpdateJob = null
            }
        }
    }

    private fun commitToModel() {
        takeOfferPresenter.commitAmount(priceQuote, quoteAmount, baseAmount)
    }
}
