package network.bisq.mobile.presentation.ui.uicases.take_offer


import kotlinx.coroutines.flow.MutableStateFlow
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

    override fun onViewAttached() {
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
        presenterScope.launch {
            launch {
                takeOfferStatus.collect { value ->
                    log.i { "takeOfferStatus: $value" }
                    //todo show state
                }
            }
            launch {
                takeOfferErrorMessage.collect { value ->
                    log.i { "takeOfferErrorMessage: $value" }
                    //todo show error
                }
            }
        }
        backgroundScope.launch {
            // TODO deactivate buttons, show waiting state
            try {
                takeOfferPresenter.takeOffer(takeOfferStatus, takeOfferErrorMessage)
            } catch (e: Exception) {
                log.e("Take offer failed", e)
                // show error to user
            }
            // TODO hide waiting state, show successfully published state, show button to open offer book, clear navigation backstack
            onNavigateToMyTrades()
        }
    }

    private fun onNavigateToMyTrades() {
        presenterScope.launch {
            // FIXME without clearing the backstack it does not work
            val rootNavController = getRootNavController()
            var currentBackStack = rootNavController.currentBackStack.value
            while (currentBackStack.size > 2) {
                rootNavController.popBackStack()
                currentBackStack = rootNavController.currentBackStack.value
            }
            navigateToTab(Routes.TabOpenTradeList)
        }
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
