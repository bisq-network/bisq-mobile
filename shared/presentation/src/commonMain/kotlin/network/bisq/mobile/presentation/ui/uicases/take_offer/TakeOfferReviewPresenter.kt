package network.bisq.mobile.presentation.ui.uicases.take_offer


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.i18n.toDisplayString
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TakeOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
) : BasePresenter(mainPresenter) {

    lateinit var headLine: String
    lateinit var quoteSidePaymentMethodDisplayString: String
    lateinit var baseSidePaymentMethodDisplayString: String
    lateinit var amountToPay: String
    lateinit var amountToReceive: String
    lateinit var fee: String
    lateinit var feeDetails: String
    lateinit var price: String
    lateinit var marketCodes: String
    lateinit var priceDetails: String

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel
    lateinit var appStrings: AppStrings

    // We pass that to the domain, which updates the state while take offer is in progress, so that we can show the status
    // or error to the user
    private val takeOfferStatus: MutableStateFlow<TakeOfferStatus?> = MutableStateFlow(null)
    private val takeOfferErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _showTakeOfferProgressDialog = MutableStateFlow(false)
    val showTakeOfferProgressDialog: StateFlow<Boolean> get() = _showTakeOfferProgressDialog
    fun setShowTakeOfferProgressDialog(value: Boolean) {
        _showTakeOfferProgressDialog.value = value
    }

    private val _showTakeOfferSuccessDialog = MutableStateFlow(false)
    val showTakeOfferSuccessDialog: StateFlow<Boolean> get() = _showTakeOfferSuccessDialog
    fun setShowTakeOfferSuccessDialog(value: Boolean) {
        _showTakeOfferSuccessDialog.value = value
    }

    override fun onViewAttached() {
        presenterScope.launch {
            takeOfferStatus.collect { value ->
                log.i { "takeOfferStatus: $value" }
                //todo show state
            }
        }
        presenterScope.launch {
            takeOfferErrorMessage.collect { message ->
                showSnackbar(message ?: "Unexpected error occurred, please try again", true)
            }
        }

        takeOfferModel = takeOfferPresenter.takeOfferModel

        quoteSidePaymentMethodDisplayString = appStrings.paymentMethod.toDisplayString(takeOfferModel.quoteSidePaymentMethod)
        baseSidePaymentMethodDisplayString = appStrings.paymentMethod.toDisplayString(takeOfferModel.baseSidePaymentMethod)

        val formattedQuoteAmount = AmountFormatter.formatAmount(takeOfferModel.quoteAmount, true, true)
        val formattedBaseAmount = AmountFormatter.formatAmount(takeOfferModel.baseAmount, false, true)

        val offerListItem = takeOfferModel.offerItemPresentationVO
        val takersDirection = offerListItem.bisqEasyOffer.direction.mirror
        headLine = "${takersDirection.name.uppercase()} Bitcoin"

        val i18n = appStrings.bisqEasyTradeWizard
        if (takersDirection.isBuy) {
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

        marketCodes = offerListItem.bisqEasyOffer.market.marketCodes
        price = offerListItem.formattedPrice.value //todo we need updated price not static offer price
        applyPriceDetails()
    }

    fun onBack() {
        navigateBack()
    }

    fun onTakeOffer() {
        backgroundScope.launch {
            setShowTakeOfferProgressDialog(true)
            // TODO deactivate buttons, show waiting state
            try {
                enableInteractive(false)
                takeOfferPresenter.takeOffer(takeOfferStatus, takeOfferErrorMessage)
            } catch (e: Exception) {
                log.e("Take offer failed", e)
                // TODO this is not working (probably the snackbar is being rendered underneath?)
                takeOfferErrorMessage.value = e.message ?: "Offer cannot be taken at this time"
            } finally {
                delay(3000L)
                setShowTakeOfferProgressDialog(false)
                setShowTakeOfferSuccessDialog(true)
                enableInteractive()
            }
            // TODO hide waiting state, show successfully published state, show button to open offer book, clear navigation backstack
            // onGoToOpenTrades()


        }
    }

    fun onGoToOpenTrades() {
        setShowTakeOfferSuccessDialog(false)
        closeWorkflow()
        // ensure we go to the my trade tab
        navigateToTab(Routes.TabOpenTradeList)
    }

    private fun closeWorkflow() {
        // TODO review better alternative than double call?
        navigateBackTo(Routes.OffersByMarket)
        navigateBack()
   }

    private fun applyPriceDetails() {
        val i18n = appStrings.bisqEasyTradeWizard
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        val percent =
            PriceUtil.findPercentFromMarketPrice(
                marketPriceServiceFacade,
                priceSpec,
                takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market
            )
        if ((priceSpec is FloatPriceSpecVO || priceSpec is MarketPriceSpecVO) && percent == 0.0) {
            priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails
        } else {
            val priceWithCode = PriceQuoteFormatter.format(takeOfferModel.priceQuote, true, true)
            val percentagePrice = PercentageFormatter.format(percent, true)
            val aboveOrBelow: String = if (percent > 0) "above" else "below" //todo
            if (priceSpec is FloatPriceSpecVO) {
                priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails_float(percentagePrice, aboveOrBelow, priceWithCode)
            } else {
                if (percent == 0.0) {
                    priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails_fix_atMarket(priceWithCode)
                } else {
                    priceDetails = i18n.bisqEasy_tradeWizard_review_priceDetails_fix(percentagePrice, aboveOrBelow, priceWithCode)
                }
            }
        }
    }

}
