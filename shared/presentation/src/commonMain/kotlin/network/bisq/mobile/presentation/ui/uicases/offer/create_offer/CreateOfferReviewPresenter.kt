package network.bisq.mobile.presentation.ui.uicases.offer.create_offer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.replicated.common.currency.marketCodes
import network.bisq.mobile.domain.replicated.offer.isBuy
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.i18n.toDisplayString
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes


class CreateOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var headLine: String
    lateinit var quoteSidePaymentMethodDisplayString: String
    lateinit var baseSidePaymentMethodDisplayString: String
    lateinit var amountToPay: String
    lateinit var amountToReceive: String
    lateinit var fee: String
    lateinit var feeDetails: String
    lateinit var formattedPrice: String
    lateinit var marketCodes: String
    lateinit var priceDetails: String

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel
    lateinit var appStrings: AppStrings

    override fun onViewAttached() {
        createOfferModel = createOfferPresenter.createOfferModel

        quoteSidePaymentMethodDisplayString =
            createOfferModel.selectedQuoteSidePaymentMethods.joinToString(", ") { appStrings.paymentMethod.toDisplayString(it) }
        baseSidePaymentMethodDisplayString =
            createOfferModel.selectedBaseSidePaymentMethods.joinToString(", ") { appStrings.paymentMethod.toDisplayString(it) }

        val formattedQuoteAmount: String
        val formattedBaseAmount: String
        if (createOfferModel.amountType == CreateOfferPresenter.AmountType.FIXED_AMOUNT) {
            formattedQuoteAmount = AmountFormatter.formatAmount(createOfferModel.quoteSideFixedAmount!!, true, true)
            formattedBaseAmount = AmountFormatter.formatAmount(createOfferModel.baseSideFixedAmount!!, false, true)
        } else {
            formattedQuoteAmount = AmountFormatter.formatRangeAmount(
                createOfferModel.quoteSideMinRangeAmount!!,
                createOfferModel.quoteSideMaxRangeAmount!!,
                true
            )
            formattedBaseAmount = AmountFormatter.formatRangeAmount(
                createOfferModel.baseSideMinRangeAmount!!,
                createOfferModel.baseSideMaxRangeAmount!!,
                false
            )
        }
        val direction = createOfferModel.direction
        headLine = "${direction.name.uppercase()} Bitcoin"

        val i18n = appStrings.bisqEasyTradeWizard
        if (direction.isBuy) {
            amountToPay = formattedQuoteAmount
            amountToReceive = formattedBaseAmount
            fee = i18n.bisqEasy_tradeWizard_review_noTradeFees
            feeDetails = i18n.bisqEasy_tradeWizard_review_sellerPaysMinerFeeLong
        } else {
            amountToPay = formattedBaseAmount
            amountToReceive = formattedQuoteAmount
            fee = i18n.bisqEasy_tradeWizard_review_sellerPaysMinerFee
            feeDetails = i18n.bisqEasy_tradeWizard_review_noTradeFeesLong
        }

        marketCodes = createOfferModel.market!!.marketCodes
        formattedPrice = PriceQuoteFormatter.format(createOfferModel.priceQuote, true, false)

        applyPriceDetails()
    }

    private fun applyPriceDetails() {
        val i18n = appStrings.bisqEasyTradeWizard
        val percentagePriceValue = createOfferModel.percentagePriceValue
        if (percentagePriceValue == 0.0) {
            priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails
        } else {
            val priceWithCode = PriceQuoteFormatter.format(createOfferModel.priceQuote, true, true)
            val percentagePrice = PercentageFormatter.format(percentagePriceValue, true)
            val aboveOrBelow: String = if (percentagePriceValue > 0) "above" else "below" //todo
            if (createOfferModel.priceType == CreateOfferPresenter.PriceType.PERCENTAGE) {
                priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails_float(percentagePrice, aboveOrBelow, priceWithCode)
            } else {
                if (percentagePriceValue == 0.0) {
                    priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails_fix_atMarket(priceWithCode)
                } else {
                    priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails_fix(percentagePrice, aboveOrBelow, priceWithCode)
                }
            }
        }
    }

    fun onBack() {
        rootNavigator.popBackStack()
    }

    fun onCreateOffer() {
        CoroutineScope(Dispatchers.Main).launch {
            // TODO deactivate buttons, show waiting state
            createOfferPresenter.createOffer()
            // TODO hide waiting state, show successfully published state, show button to open offer book, clear navigation backstack
            onGoToOfferList()
        }
    }

    fun onGoToOfferList() {
        rootNavigator.navigate(Routes.Offerbook.name)
    }
}
