package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.parser.PercentageParser
import network.bisq.mobile.domain.parser.PriceParser
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.PriceType

class CreateOfferPricePresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    var priceTypeTitle: String
    var fixPriceDescription: String
    var priceTypes: List<PriceType> = PriceType.entries.toList()
    var priceQuote: PriceQuoteVO
    var percentagePriceValue: Double = 0.0
    private val _formattedPercentagePrice = MutableStateFlow("")
    val formattedPercentagePrice: StateFlow<String> get() = _formattedPercentagePrice.asStateFlow()
    private val _formattedPercentagePriceValid = MutableStateFlow(true)
    val formattedPercentagePriceValid: StateFlow<Boolean> get() = _formattedPercentagePriceValid.asStateFlow()

    private val _formattedPrice = MutableStateFlow("")
    val formattedPrice: StateFlow<String> get() = _formattedPrice.asStateFlow()
    private val _formattedPriceValid = MutableStateFlow(true)

    private val _priceType = MutableStateFlow(PriceType.PERCENTAGE)
    val priceType: StateFlow<PriceType> get() = _priceType.asStateFlow()

    private val _isBuy: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isBuy: StateFlow<Boolean> get() = _isBuy.asStateFlow()
    private val _hintText = MutableStateFlow("")
    val hintText: StateFlow<String> get() = _hintText.asStateFlow()

    private var createOfferModel: CreateOfferPresenter.CreateOfferModel

    private val _showWhyPopup: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showWhyPopup: StateFlow<Boolean> get() = _showWhyPopup.asStateFlow()
    fun setShowWhyPopup(newValue: Boolean) {
        _showWhyPopup.value = newValue
    }

    init {
        createOfferModel = createOfferPresenter.createOfferModel

        _priceType.value = createOfferModel.priceType
        priceQuote = createOfferModel.priceQuote
        percentagePriceValue = createOfferModel.percentagePriceValue
        _formattedPercentagePrice.value = PercentageFormatter.format(percentagePriceValue, false)
        _formattedPrice.value = PriceQuoteFormatter.format(priceQuote)
        priceTypeTitle = if (priceType.value == PriceType.PERCENTAGE)
            "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage".i18n()
        else
            "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed".i18n()

        fixPriceDescription = "bisqEasy.price.tradePrice.inputBoxText".i18n(createOfferModel.market!!.marketCodes)

        _isBuy.value = createOfferModel.direction.isBuy

        if (isBuy.value) {
            updateHintText(percentagePriceValue)
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        observeMarketPriceChanges()
    }

    private fun observeMarketPriceChanges() {
        collectIO(marketPriceServiceFacade.selectedMarketPriceItem) { marketPriceItem ->
            runCatching {
                if (marketPriceItem != null) {
                    revalidateCurrentPrices()
                }
            }.onFailure { e ->
                log.e(e) { "Failed to process market price change: ${e.message}" }
                showSnackbar("mobile.bisqEasy.tradeWizard.price.updateError".i18n(), isError = true)
            }
        }
    }

    private fun revalidateCurrentPrices() {
        try {
            val isCurrentlyValid = isValid(percentagePriceValue)

            if (!isCurrentlyValid) {
                // Auto-adjust to valid range
                val adjustedPercentage = percentagePriceValue.coerceIn(-0.1, 0.5)
                log.i { "Auto-adjusting price from ${percentagePriceValue * 100}% to ${adjustedPercentage * 100}% due to market price change" }

                // Reuse existing method to update both percentage and fixed price consistently
                val adjustedPercentageString = PercentageFormatter.format(adjustedPercentage, false)
                onPercentagePriceChanged(adjustedPercentageString, true)

                showSnackbar("mobile.bisqEasy.tradeWizard.price.adjustedDueToMarketChange".i18n())
            } else {
                // Price is still valid, but we need to update the calculated values based on new market price
                if (priceType.value == PriceType.PERCENTAGE) {
                    // For percentage price, recalculate the fixed price with new market price
                    val currentPercentageString = _formattedPercentagePrice.value
                    onPercentagePriceChanged(currentPercentageString, true)
                } else {
                    // For fixed price, recalculate the percentage with new market price
                    val currentFixedPriceString = _formattedPrice.value
                    onFixPriceChanged(currentFixedPriceString, true)
                }
            }

        } catch (e: Exception) {
            log.e(e) { "Failed to revalidate prices after market price change: ${e.message}" }
        }
    }

    fun getPriceTypeDisplayString(priceType: PriceType): String {
        return if (priceType == PriceType.PERCENTAGE)
            "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage".i18n()
        else
            "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed".i18n()
    }

    fun onSelectPriceType(value: PriceType) {
        _priceType.value = value
    }

    fun onPercentagePriceChanged(value: String, isValid: Boolean) {
        try {
            percentagePriceValue = PercentageParser.parse(value)
            _formattedPercentagePrice.value = PercentageFormatter.format(this.percentagePriceValue, false)
            val marketPriceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
            priceQuote = PriceUtil.fromMarketPriceMarkup(marketPriceQuote, this.percentagePriceValue)
            _formattedPrice.value = PriceQuoteFormatter.format(priceQuote)
        } catch (e: Exception) {
            log.e(e) { "Failed to process percentage price change: ${e.message}" }
            _formattedPercentagePriceValid.value = false
            _formattedPriceValid.value = false
            return
        }

        _formattedPercentagePriceValid.value = isValid
        _formattedPriceValid.value = isValid

        if (isBuy.value) {
            updateHintText(percentagePriceValue)
        }
    }

    fun onFixPriceChanged(value: String, isValid: Boolean) {
        try {
            // Validate input and market
            if (value.isBlank()) {
                log.w { "Empty value provided to onFixPriceChanged" }
                _formattedPercentagePriceValid.value = false
                _formattedPriceValid.value = false
                return
            }

            val market = createOfferModel.market
            if (market == null) {
                log.e { "Market is null in onFixPriceChanged" }
                _formattedPercentagePriceValid.value = false
                _formattedPriceValid.value = false
                return
            }

            // Use safe parsing
            val valueAsDouble = PriceParser.parseOrNull(value)
            if (valueAsDouble == null) {
                log.w { "Invalid price format: $value" }
                _formattedPercentagePriceValid.value = false
                _formattedPriceValid.value = false
                return
            }

            if (valueAsDouble <= 0.0 || !valueAsDouble.isFinite()) {
                log.w { "Invalid price value: $valueAsDouble" }
                _formattedPercentagePriceValid.value = false
                _formattedPriceValid.value = false
                return
            }

            // Create price quote and calculate percentage
            priceQuote = PriceQuoteVOFactory.fromPrice(valueAsDouble, market)
            _formattedPrice.value = PriceQuoteFormatter.format(priceQuote)

            val marketPriceQuote = createOfferPresenter.getMostRecentPriceQuote(market)
            if (marketPriceQuote.value <= 0) {
                log.e { "Invalid market price: ${marketPriceQuote.value}" }
                _formattedPercentagePriceValid.value = false
                _formattedPriceValid.value = false
                return
            }

            percentagePriceValue = PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote)
            if (!percentagePriceValue.isFinite()) {
                log.e { "Invalid percentage calculation result: $percentagePriceValue" }
                _formattedPercentagePriceValid.value = false
                _formattedPriceValid.value = false
                return
            }

            _formattedPercentagePrice.value = PercentageFormatter.format(percentagePriceValue, false)
            _formattedPercentagePriceValid.value = isValid
            _formattedPriceValid.value = isValid

            if (isBuy.value) {
                val percentageValue = PriceUtil.findPercentFromMarketPrice(
                    marketPriceServiceFacade,
                    FixPriceSpecVO(priceQuote),
                    market,
                )

                updateHintText(percentageValue)
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to process fixed price change: ${e.message}" }
            _formattedPercentagePriceValid.value = false
            _formattedPriceValid.value = false
        }
    }

    fun calculatePercentageForFixedValue(value: String): Double {
        try {
            if (value.isBlank()) {
                log.w { "Empty value provided to calculatePercentageForFixedValue" }
                return 0.0
            }
            val market = createOfferModel.market
            if (market == null) {
                log.e { "Market is null in calculatePercentageForFixedValue" }
                return 0.0
            }
            val valueAsDouble = PriceParser.parseOrNull(value)
            if (valueAsDouble == null) {
                log.w { "Invalid price format: $value" }
                return 0.0
            }
            if (valueAsDouble <= 0.0 || !valueAsDouble.isFinite()) {
                log.w { "Invalid price value: $valueAsDouble" }
                return 0.0
            }

            priceQuote = PriceQuoteVOFactory.fromPrice(valueAsDouble, market)
            val marketPriceQuote = createOfferPresenter.getMostRecentPriceQuote(market)
            if (marketPriceQuote.value <= 0) {
                log.e { "Invalid market price: ${marketPriceQuote.value}" }
                return 0.0
            }

            percentagePriceValue = PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote)
            if (!percentagePriceValue.isFinite()) {
                log.e { "Invalid percentage calculation result: $percentagePriceValue" }
                return 0.0
            }

            return percentagePriceValue
        } catch (e: Exception) {
            log.e(e) { "Failed to calculate percentage for fixed value: ${e.message}" }
            return 0.0
        }
    }

    private fun updateHintText(percentageValue: Double) {

        val feedbackRating = if (percentageValue < -0.05) {
            "bisqEasy.price.feedback.sentence.veryLow".i18n()
        } else if (percentageValue < 0) {
            "bisqEasy.price.feedback.sentence.low".i18n()
        } else if (percentageValue < 0.05) {
            "bisqEasy.price.feedback.sentence.some".i18n()
        } else if (percentageValue < 0.15) {
            "bisqEasy.price.feedback.sentence.good".i18n()
        } else {
            "bisqEasy.price.feedback.sentence.veryGood".i18n()
        }

        _hintText.value = "bisqEasy.price.feedback.buyOffer.sentence".i18n(feedbackRating)
    }

    fun onBack() {
        if (isValid(percentagePriceValue)) {
            commitToModel()
        }
        navigateBack()
    }


    fun onClose() {
        if (isValid(percentagePriceValue)) {
            commitToModel()
        }
        navigateToOfferbookTab()
    }

    fun onNext() {
        if (isValid(percentagePriceValue)) {
            commitToModel()
            navigateTo(Routes.CreateOfferQuoteSidePaymentMethod)
        }
    }

    private fun commitToModel() {
        createOfferPresenter.commitPrice(
            priceType.value, percentagePriceValue, priceQuote
        )
    }

    private fun isValid(percentagePriceValue: Double): Boolean {
        return percentagePriceValue >= -0.1 && percentagePriceValue <= 0.5
    }

}
