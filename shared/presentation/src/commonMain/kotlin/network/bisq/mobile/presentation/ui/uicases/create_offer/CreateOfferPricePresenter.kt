package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    lateinit var priceTypeTitle: String
    lateinit var fixPriceDescription: String
    var priceTypes: List<PriceType> = PriceType.entries.toList()
    lateinit var priceQuote: PriceQuoteVO
    var percentagePriceValue: Double = 0.0
    private val _formattedPercentagePrice = MutableStateFlow("")
    val formattedPercentagePrice: StateFlow<String> = _formattedPercentagePrice
    private val _formattedPercentagePriceValid = MutableStateFlow(true)
    val formattedPercentagePriceValid: StateFlow<Boolean> = _formattedPercentagePriceValid

    private val _formattedPrice = MutableStateFlow("")
    val formattedPrice: StateFlow<String> = _formattedPrice
    private val _formattedPriceValid = MutableStateFlow(true)
    val formattedPriceValid: StateFlow<Boolean> = _formattedPriceValid

    private val _priceType = MutableStateFlow(PriceType.PERCENTAGE)
    val priceType: StateFlow<PriceType> = _priceType

    private var _isBuy: MutableStateFlow<Boolean> = MutableStateFlow(true)
    var isBuy: StateFlow<Boolean> = _isBuy
    private val _hintText = MutableStateFlow("")
    val hintText: StateFlow<String> = _hintText

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel

    private var _showWhyPopup: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showWhyPopup: StateFlow<Boolean> = _showWhyPopup
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
            "Percentage" //TODO:i18n
        else
            "Fixed" //TODO:i18n

        fixPriceDescription = "bisqEasy.price.tradePrice.inputBoxText".i18n(createOfferModel.market!!.marketCodes)

        _isBuy.value = createOfferModel.direction.isBuy
        //onPercentagePriceChanged("10")

        if (isBuy.value) {
            updateHintText(percentagePriceValue)
        }
    }

    fun getPriceTypeDisplayString(priceType: PriceType): String {
        return if (priceType == PriceType.PERCENTAGE)
            "Percentage" //TODO:i18n
        else
            "Fixed" //TODO:i18n
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
        } catch (_: Exception) {

        }

        _formattedPercentagePriceValid.value = isValid
        _formattedPriceValid.value = isValid

        if (isBuy.value) {
            updateHintText(percentagePriceValue)
        }
    }

    fun onFixPriceChanged(value: String, isValid: Boolean) {
        val valueAsDouble = PriceParser.parse(value)
        priceQuote = PriceQuoteVOFactory.fromPrice(valueAsDouble, createOfferModel.market!!)
        _formattedPrice.value = PriceQuoteFormatter.format(priceQuote)
        val marketPriceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        percentagePriceValue = PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote)
        _formattedPercentagePrice.value = PercentageFormatter.format(percentagePriceValue, false)

        _formattedPercentagePriceValid.value = isValid
        _formattedPriceValid.value = isValid

        if (isBuy.value) {
            val percentageValue = PriceUtil.findPercentFromMarketPrice(
                marketPriceServiceFacade,
                FixPriceSpecVO(priceQuote),
                createOfferModel.market!!,
            )

            updateHintText(percentageValue)
        }

    }

    fun calculatePercentageForFixedValue(value: String): Double {
        val valueAsDouble = PriceParser.parse(value)
        priceQuote = PriceQuoteVOFactory.fromPrice(valueAsDouble, createOfferModel.market!!)
        val marketPriceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        percentagePriceValue = PriceUtil.getPercentageToMarketPrice(marketPriceQuote, priceQuote)
        return percentagePriceValue * 100
    }

    fun updateHintText(percentageValue: Double) {

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

    fun onNext() {
        if (isValid(percentagePriceValue)) {
            commitToModel()
            navigateTo(Routes.CreateOfferPaymentMethod)
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
