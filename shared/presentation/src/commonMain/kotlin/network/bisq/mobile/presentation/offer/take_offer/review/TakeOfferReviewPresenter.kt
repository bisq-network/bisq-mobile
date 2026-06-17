package network.bisq.mobile.presentation.offer.take_offer.review

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.displayString
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.domain.utils.StringUtils.truncate
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.i18NPaymentMethod
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.OfferFlowPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import kotlin.math.abs

class TakeOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val takeOfferCoordinator: TakeOfferCoordinator,
) : OfferFlowPresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.TakeOfferReview

    var headLine: String
    var quoteSidePaymentMethodDisplayString: String
    var baseSidePaymentMethodDisplayString: String
    var amountToPay: String
    var amountToReceive: String
    var fee: String
    var feeDetails: String
    var price: String
    var marketCodes: String
    var takersDirection: DirectionEnum
    lateinit var priceDetails: String

    private var takeOfferModel: TakeOfferCoordinator.TakeOfferModel

    // We pass that to the domain, which updates the state while take offer is in progress, so that we can show the status
    // or error to the user
    private val takeOfferStatus: MutableStateFlow<TakeOfferStatus?> = MutableStateFlow(null)
    private val takeOfferErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _showTakeOfferProgressDialog = MutableStateFlow(false)
    val showTakeOfferProgressDialog: StateFlow<Boolean> = _showTakeOfferProgressDialog.asStateFlow()

    private fun setShowTakeOfferProgressDialog(value: Boolean) {
        _showTakeOfferProgressDialog.value = value
    }

    private val _showTakeOfferSuccessDialog = MutableStateFlow(false)
    val showTakeOfferSuccessDialog: StateFlow<Boolean> = _showTakeOfferSuccessDialog.asStateFlow()

    private fun setShowTakeOfferSuccessDialog(value: Boolean) {
        _showTakeOfferSuccessDialog.value = value
    }

    // Atomic guard against rapid-fire taps on the "Take offer" button. The progress
    // dialog is the visible signal, but its modality is not enough on its own —
    // touches can land before the dialog renders, especially on the android node
    // app where the API returns near-instantly. compareAndSet ensures only the
    // first tap proceeds; subsequent taps short-circuit until SUCCESS / error.
    private val isTakingOffer = MutableStateFlow(false)

    init {
        presenterScope.launch {
            takeOfferStatus.collect {
                log.i { "takeOfferStatus: $it" }
                if (it == TakeOfferStatus.SUCCESS) {
                    setShowTakeOfferSuccessDialog(true)
                    setShowTakeOfferProgressDialog(false)
                    // Keep isTakingOffer = true on success: the user moves on via
                    // the success dialog. Resetting could re-expose the button if
                    // the dialog were dismissed unexpectedly.
                }
            }
        }
        presenterScope.launch {
            // To ignore the first init message
            takeOfferErrorMessage.drop(1).collect {
                log.e { "takeOfferErrorMessage: $it" }
                showSnackbar(it ?: "mobile.takeOffer.unexpectedError".i18n(), type = SnackbarType.ERROR)
                // Error path: hide the progress dialog (otherwise it stays up forever)
                // and release the guard so the user can retry.
                setShowTakeOfferProgressDialog(false)
                isTakingOffer.value = false
            }
        }

        takeOfferModel = takeOfferCoordinator.takeOfferModel
        val offerListItem = takeOfferModel.offerItemPresentationVO
        takersDirection = offerListItem.bisqEasyOffer.direction.mirror

        quoteSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.quoteSidePaymentMethod).first
        baseSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.baseSidePaymentMethod).first

        val formattedQuoteAmount = AmountFormatter.formatAmount(takeOfferModel.quoteAmount, true, true)
        val formattedBaseAmount = AmountFormatter.formatAmount(takeOfferModel.baseAmount, false, false)

        headLine = "${translatedDirection()} Bitcoin"

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
        price = PriceQuoteFormatter.format(takeOfferModel.priceQuote, true, false)
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
        if (isDemo()) {
            showSnackbar("mobile.demo.action.disabled".i18n(), type = SnackbarType.ERROR)
            return
        }
        if (!isTakingOffer.compareAndSet(expect = false, update = true)) {
            log.w { "onTakeOffer called while a take is already in progress; ignoring" }
            return
        }
        setShowTakeOfferProgressDialog(true)
        presenterScope.launch {
            try {
                val (statusFlow, errorFlow) = takeOfferCoordinator.takeOffer()

                // The stateFlow objects are set in the ioScope in the service. Thus we need to map them to the presenterScope.
                presenterScope.launch {
                    statusFlow.collect { takeOfferStatus.value = it }
                }
                presenterScope.launch {
                    errorFlow.collect { takeOfferErrorMessage.value = it }
                }
            } catch (e: Exception) {
                log.e("Take offer failed", e)
                takeOfferErrorMessage.value =
                    e.message ?: ("mobile.takeOffer.failedWithException".i18n(e.toString().truncate(50)))
                setShowTakeOfferProgressDialog(false)
                isTakingOffer.value = false
            }
        }
    }

    fun onGoToOpenTrades() {
        setShowTakeOfferSuccessDialog(false)
        navigateToTab(NavRoute.TabMyTrades(NavRoute.TabMyTrades.TAB_OPEN))
    }

    private fun applyPriceDetails() {
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        val percent =
            PriceUtil.findPercentFromMarketPrice(
                marketPriceServiceFacade,
                priceSpec,
                takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market,
            )
        if ((priceSpec is FloatPriceSpecVO || priceSpec is MarketPriceSpecVO) && percent == 0.0) {
            priceDetails = "bisqEasy.tradeWizard.review.priceDetails".i18n()
        } else {
            val priceWithCode = PriceQuoteFormatter.format(takeOfferModel.originalPriceQuote, true, true)
            val percentagePrice = PercentageFormatter.format(abs(percent), true)
            val aboveOrBelow: String = if (percent > 0) "mobile.general.above".i18n() else "mobile.general.below".i18n()
            priceDetails =
                if (priceSpec is FloatPriceSpecVO) {
                    "bisqEasy.tradeWizard.review.priceDetails.float".i18n(percentagePrice, aboveOrBelow, priceWithCode)
                } else {
                    if (percent == 0.0) {
                        "bisqEasy.tradeWizard.review.priceDetails.fix.atMarket".i18n(priceWithCode)
                    } else {
                        "bisqEasy.tradeWizard.review.priceDetails.fix".i18n(
                            percentagePrice,
                            aboveOrBelow,
                            priceWithCode,
                        )
                    }
                }
        }
    }

    private fun translatedDirection(): String = takersDirection.displayString.uppercase()
}
