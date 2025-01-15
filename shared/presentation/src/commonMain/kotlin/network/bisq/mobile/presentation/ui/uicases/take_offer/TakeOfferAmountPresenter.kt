package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import kotlin.math.roundToLong

class TakeOfferAmountPresenter(
    mainPresenter: MainPresenter,
    private val takeOfferPresenter: TakeOfferPresenter
) : BasePresenter(mainPresenter) {

    var sliderPosition: Float = 0.5f
    lateinit var quoteCurrencyCode: String
    lateinit var formattedMinAmount: String
    lateinit var formattedMinAmountWithCode: String
    lateinit var formattedMaxAmountWithCode: String
    private val _formattedQuoteAmount = MutableStateFlow("")
    val formattedQuoteAmount: StateFlow<String> = _formattedQuoteAmount
    private val _formattedBaseAmount = MutableStateFlow("")
    val formattedBaseAmount: StateFlow<String> = _formattedBaseAmount

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel
    private var minAmount: Long = getMinAmountValue()
    private var maxAmount: Long = getMaxAmountValue()
    private lateinit var priceQuote: PriceQuoteVO
    private lateinit var quoteAmount: FiatVO
    private lateinit var baseAmount: CoinVO

    override fun onViewAttached() {
        takeOfferModel = takeOfferPresenter.takeOfferModel
        val offerListItem = takeOfferModel.offerItemPresentationVO
        quoteCurrencyCode = offerListItem.bisqEasyOffer.market.quoteCurrencyCode

        val rangeAmountSpec: RangeAmountSpecVO =
            offerListItem.bisqEasyOffer.amountSpec as RangeAmountSpecVO
        minAmount = maxOf(getMinAmountValue(), rangeAmountSpec.minAmount)
        maxAmount = minOf(getMaxAmountValue(), rangeAmountSpec.maxAmount)

        formattedMinAmount = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode))
        formattedMinAmountWithCode = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode), true, true)
        formattedMaxAmountWithCode = AmountFormatter.formatAmount(FiatVOFactory.from(maxAmount, quoteCurrencyCode), true, true)

        _formattedQuoteAmount.value = offerListItem.formattedQuoteAmount
        _formattedBaseAmount.value = offerListItem.formattedBaseAmount.value

        sliderPosition = 0.5f
        applySliderValue(sliderPosition)
    }

    //todo convert to selected currency from USD value
    private fun getMaxAmountValue() = MAX_USD_TRADE_AMOUNT.value

    private fun getMinAmountValue() = DEFAULT_MIN_USD_TRADE_AMOUNT.value

    fun onSliderValueChanged(sliderPosition: Float) {
        applySliderValue(sliderPosition)
    }

    fun onTextValueChanged(textInput: String) {
        //todo parse input string and apply it to model
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        commitToModel()

        if (takeOfferPresenter.showPaymentMethodsScreen()) {
            navigateTo(Routes.TakeOfferPaymentMethod)
        } else {
            navigateTo(Routes.TakeOfferReviewTrade)
        }
    }

    private fun applySliderValue(sliderPosition: Float) {
        this.sliderPosition = sliderPosition
        val range = maxAmount - minAmount
        val value: Float = minAmount + (sliderPosition * range)
        val roundedFiatValue: Long = (value / 10000.0f).roundToLong() * 10000

        // We do not apply the data to the model yet to avoid unnecessary model clones
        quoteAmount = FiatVOFactory.from(roundedFiatValue, quoteCurrencyCode)
        _formattedQuoteAmount.value = AmountFormatter.formatAmount(quoteAmount)

        priceQuote = takeOfferPresenter.getMostRecentPriceQuote()
        baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
        _formattedBaseAmount.value = AmountFormatter.formatAmount(baseAmount, false)
    }

    private fun commitToModel() {
        takeOfferPresenter.commitAmount(priceQuote, quoteAmount, baseAmount)
    }
}
