package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType
import kotlin.math.roundToLong

class CreateOfferAmountPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter,
) : BasePresenter(mainPresenter) {

    lateinit var headline: String
    lateinit var quoteCurrencyCode: String
    lateinit var formattedMinAmount: String

    val _amountType: MutableStateFlow<AmountType> = MutableStateFlow(AmountType.FIXED_AMOUNT)
    val amountType: StateFlow<AmountType> = _amountType

    // FIXED_AMOUNT
    var fixedAmountSliderPosition: Float = 0.5f
    var formattedMinAmountWithCode: String = ""
    var formattedMaxAmountWithCode: String = ""
    private val _formattedQuoteSideFixedAmount = MutableStateFlow("")
    val formattedQuoteSideFixedAmount: StateFlow<String> = _formattedQuoteSideFixedAmount
    private val _formattedBaseSideFixedAmount = MutableStateFlow("")
    val formattedBaseSideFixedAmount: StateFlow<String> = _formattedBaseSideFixedAmount

    // RANGE_AMOUNT
    var rangeSliderPosition: ClosedFloatingPointRange<Float> = 0.0f..1.0f
    private val _formattedQuoteSideMinRangeAmount = MutableStateFlow("")
    val formattedQuoteSideMinRangeAmount: StateFlow<String> = _formattedQuoteSideMinRangeAmount
    private val _formattedBaseSideMinRangeAmount = MutableStateFlow("")
    val formattedBaseSideMinRangeAmount: StateFlow<String> = _formattedBaseSideMinRangeAmount

    private val _formattedQuoteSideMaxRangeAmount = MutableStateFlow("")
    val formattedQuoteSideMaxRangeAmount: StateFlow<String> = _formattedQuoteSideMaxRangeAmount
    private val _formattedBaseSideMaxRangeAmount = MutableStateFlow("")
    val formattedBaseSideMaxRangeAmount: StateFlow<String> = _formattedBaseSideMaxRangeAmount

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel
    private var minAmount: Long = DEFAULT_MIN_USD_TRADE_AMOUNT.value
    private var maxAmount: Long = MAX_USD_TRADE_AMOUNT.value
    private lateinit var priceQuote: PriceQuoteVO
    private lateinit var quoteSideFixedAmount: FiatVO
    private lateinit var baseSideFixedAmount: CoinVO
    private lateinit var quoteSideMinRangeAmount: FiatVO
    private lateinit var baseSideMinRangeAmount: CoinVO
    private lateinit var quoteSideMaxRangeAmount: FiatVO
    private lateinit var baseSideMaxRangeAmount: CoinVO

    lateinit var appStrings: AppStrings

    override fun onViewAttached() {
        createOfferModel = createOfferPresenter.createOfferModel
        quoteCurrencyCode = createOfferModel.market!!.quoteCurrencyCode
        _amountType.value = createOfferModel.amountType

        headline = if (createOfferModel.direction.isBuy)
            appStrings.bisqEasyTradeWizard.bisqEasy_tradeWizard_amount_headline_buyer
        else
            appStrings.bisqEasyTradeWizard.bisqEasy_tradeWizard_amount_headline_seller

        minAmount = BisqEasyTradeAmountLimits.getMinAmountValue(marketPriceServiceFacade, quoteCurrencyCode)
        maxAmount = BisqEasyTradeAmountLimits.getMaxAmountValue(marketPriceServiceFacade, quoteCurrencyCode)

        formattedMinAmount = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode))
        formattedMinAmountWithCode = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode), true, true)
        formattedMaxAmountWithCode = AmountFormatter.formatAmount(FiatVOFactory.from(maxAmount, quoteCurrencyCode), true, true)

        fixedAmountSliderPosition = createOfferModel.fixedAmountSliderPosition
        applyFixedAmountSliderValue(fixedAmountSliderPosition)

        rangeSliderPosition = createOfferModel.rangeSliderPosition
        applyRangeAmountSliderValue(rangeSliderPosition)
    }

    fun onSelectAmountType(value: AmountType) {
        _amountType.value = value
    }

    fun onFixedAmountTextValueChanged(textInput: String) {
        //todo parse input string and apply it to model
    }

    fun onMinAmountTextValueChanged(textInput: String) {
        //todo parse input string and apply it to model
    }

    fun onMaxAmountTextValueChanged(textInput: String) {
        //todo parse input string and apply it to model
    }

    fun onFixedAmountSliderChanged(value: Float) {
        applyFixedAmountSliderValue(value)
    }

    fun onRangeAmountSliderChanged(value: ClosedFloatingPointRange<Float>) {
        applyRangeAmountSliderValue(value)
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        commitToModel()
        navigateTo(Routes.CreateOfferPrice)
    }

    private fun applyRangeAmountSliderValue(rangeSliderPosition: ClosedFloatingPointRange<Float>) {
        this.rangeSliderPosition = rangeSliderPosition

        val range = maxAmount - minAmount
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)

        val min = rangeSliderPosition.start;
        val minValue: Float = minAmount + (min * range)
        val roundedMinQuoteValue: Long = (minValue / 10000f).roundToLong() * 10000

        quoteSideMinRangeAmount = FiatVOFactory.from(roundedMinQuoteValue, quoteCurrencyCode)
        _formattedQuoteSideMinRangeAmount.value = AmountFormatter.formatAmount(quoteSideMinRangeAmount)

        baseSideMinRangeAmount =
            priceQuote.toBaseSideMonetary(quoteSideMinRangeAmount) as network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
        _formattedBaseSideMinRangeAmount.value = AmountFormatter.formatAmount(baseSideMinRangeAmount, false)

        val max = rangeSliderPosition.endInclusive
        val maxValue: Float = minAmount + (max * range)
        val roundedMaxQuoteValue: Long = (maxValue / 10000f).roundToLong() * 10000

        quoteSideMaxRangeAmount = FiatVOFactory.from(roundedMaxQuoteValue, quoteCurrencyCode)
        _formattedQuoteSideMaxRangeAmount.value = AmountFormatter.formatAmount(quoteSideMaxRangeAmount)

        baseSideMaxRangeAmount =
            priceQuote.toBaseSideMonetary(quoteSideMaxRangeAmount) as network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
        _formattedBaseSideMaxRangeAmount.value = AmountFormatter.formatAmount(baseSideMaxRangeAmount, false)
    }

    private fun applyFixedAmountSliderValue(fixedAmountSliderPosition: Float) {
        this.fixedAmountSliderPosition = fixedAmountSliderPosition

        val range = maxAmount - minAmount
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)

        val value: Float = minAmount + (fixedAmountSliderPosition * range)
        val roundedQuoteValue: Long = (value / 10000f).roundToLong() * 10000

        // We do not apply the data to the model yet to avoid unnecessary model clones
        quoteSideFixedAmount = FiatVOFactory.from(roundedQuoteValue, quoteCurrencyCode)
        _formattedQuoteSideFixedAmount.value = AmountFormatter.formatAmount(quoteSideFixedAmount)

        baseSideFixedAmount =
            priceQuote.toBaseSideMonetary(quoteSideFixedAmount) as network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
        _formattedBaseSideFixedAmount.value = AmountFormatter.formatAmount(baseSideFixedAmount, false)
    }

    private fun commitToModel() {
        createOfferPresenter.commitAmount(
            amountType.value,
            quoteSideFixedAmount,
            baseSideFixedAmount,
            quoteSideMinRangeAmount,
            baseSideMinRangeAmount,
            quoteSideMaxRangeAmount,
            baseSideMaxRangeAmount
        )
    }
}
