package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.domain.utils.StringUtils.truncate
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
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
    lateinit var takersDirection: DirectionEnum

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel

    // We pass that to the domain, which updates the state while take offer is in progress, so that we can show the status
    // or error to the user
    private val takeOfferStatus: MutableStateFlow<TakeOfferStatus?> = MutableStateFlow(null)
    private val takeOfferErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _showTakeOfferProgressDialog = MutableStateFlow(false)
    val showTakeOfferProgressDialog: StateFlow<Boolean> get() = _showTakeOfferProgressDialog.asStateFlow()
    private fun setShowTakeOfferProgressDialog(value: Boolean) {
        _showTakeOfferProgressDialog.value = value
    }

    private val _showTakeOfferSuccessDialog = MutableStateFlow(false)
    val showTakeOfferSuccessDialog: StateFlow<Boolean> get() = _showTakeOfferSuccessDialog.asStateFlow()
    private fun setShowTakeOfferSuccessDialog(value: Boolean) {
        _showTakeOfferSuccessDialog.value = value
    }

    init {
        collectUI(takeOfferStatus) {
            log.i { "takeOfferStatus: $it" }
            if (it == TakeOfferStatus.SUCCESS) {
                setShowTakeOfferSuccessDialog(true)
            }
        }
        // To ignore the first init message
        collectUI(takeOfferErrorMessage.drop(1)) {
            log.e { "takeOfferErrorMessage: $it" }
            showSnackbar(it ?: "mobile.takeOffer.unexpectedError".i18n(), true)
        }

        takeOfferModel = takeOfferPresenter.takeOfferModel
        val offerListItem = takeOfferModel.offerItemPresentationVO
        takersDirection = offerListItem.bisqEasyOffer.direction.mirror

        quoteSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.quoteSidePaymentMethod).first
        baseSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.baseSidePaymentMethod).first

        val formattedQuoteAmount = AmountFormatter.formatAmount(takeOfferModel.quoteAmount, true, true)
        val formattedBaseAmount = AmountFormatter.formatAmount(takeOfferModel.baseAmount, false, false)

        headLine = "${takersDirection.name.uppercase()} Bitcoin"

        if (takersDirection.isBuy) {
            amountToPay = formattedQuoteAmount
            amountToReceive = formattedBaseAmount
            fee = "bisqEasy.tradeWizard.review.noTradeFees".i18n()
            feeDetails = "bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong".i18n()
        } else {
            amountToPay = formattedBaseAmount
            amountToReceive = formattedQuoteAmount
            fee = "bisqEasy.tradeWizard.review.sellerPaysMinerFee".i18n()
            feeDetails = "bisqEasy.tradeWizard.review.noTradeFeesLong".i18n()
        }

        marketCodes = offerListItem.bisqEasyOffer.market.marketCodes
        price = MarketPriceFormatter.format(takeOfferModel.priceQuote.value, takeOfferModel.priceQuote.market)
        applyPriceDetails()
    }

    override fun onViewUnattaching() {
        super.onViewUnattaching()
    }

    fun onBack() {
        navigateBack()
    }

    fun onClose() {
        navigateToOfferbookTab()
    }

    fun onTakeOffer() {
        setShowTakeOfferProgressDialog(true)
        disableInteractive()

        launchUI {
            try {
                if (isDemo()) {
                    showSnackbar("Take offer is disabled in demo mode")
                } else {
                    // takeOffer use withContext(IODispatcher) for calling the service
                    val (statusFlow, errorFlow) = takeOfferPresenter.takeOffer()

                    // The stateFlow objects are set in the ioScope in the service. Thus we need to map them to the presenterScope.
                    collectUI(statusFlow) { takeOfferStatus.value = it }
                    collectUI(errorFlow) { takeOfferErrorMessage.value = it }
                }
            } catch (e: Exception) {
                log.e("Take offer failed", e)
                takeOfferErrorMessage.value =
                    e.message ?: ("mobile.takeOffer.failedWithException".i18n(e.toString().truncate(50)))
            } finally {
                setShowTakeOfferProgressDialog(false)
                enableInteractive()
            }
        }
    }

    fun onGoToOpenTrades() {
        setShowTakeOfferSuccessDialog(false)
        navigateToOfferbookTab()
        // ensure we go to the my trade tab
        navigateToTab(Routes.TabOpenTradeList)
    }

    private fun applyPriceDetails() {
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        val percent =
            PriceUtil.findPercentFromMarketPrice(
                marketPriceServiceFacade,
                priceSpec,
                takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market
            )
        if ((priceSpec is FloatPriceSpecVO || priceSpec is MarketPriceSpecVO) && percent == 0.0) {
            priceDetails = "bisqEasy.tradeWizard.review.priceDetails".i18n()
        } else {
            val priceWithCode = PriceQuoteFormatter.format(takeOfferModel.originalPriceQuote, true, true)
            val percentagePrice = PercentageFormatter.format(percent, true)
            val aboveOrBelow: String = if (percent > 0) "above" else "below" //todo
            priceDetails = if (priceSpec is FloatPriceSpecVO) {
                "bisqEasy.tradeWizard.review.priceDetails.float".i18n(percentagePrice, aboveOrBelow, priceWithCode)
            } else {
                if (percent == 0.0) {
                    "bisqEasy.tradeWizard.review.priceDetails.fix.atMarket".i18n(priceWithCode)
                } else {
                    "bisqEasy.tradeWizard.review.priceDetails.fix".i18n(percentagePrice, aboveOrBelow, priceWithCode)
                }
            }
        }
    }

}
