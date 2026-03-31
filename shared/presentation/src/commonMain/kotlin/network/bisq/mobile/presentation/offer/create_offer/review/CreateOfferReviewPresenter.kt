package network.bisq.mobile.presentation.offer.create_offer.review

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.utils.i18NPaymentMethod
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import kotlin.math.abs

class CreateOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val createOfferCoordinator: CreateOfferCoordinator,
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
    lateinit var direction: DirectionEnum
    var formattedBaseRangeMinAmount: String = ""
    var formattedBaseRangeMaxAmount: String = ""
    var isRangeOffer: Boolean = false

    override val blockInteractivityOnAttached: Boolean = true

    private val _isCreateOfferBtnEnabled = MutableStateFlow(true)
    val isCreateOfferBtnEnabled = _isCreateOfferBtnEnabled.asStateFlow()

    private lateinit var createOfferModel: CreateOfferCoordinator.CreateOfferModel

    override fun onViewAttached() {
        super.onViewAttached()
        createOfferModel = createOfferCoordinator.createOfferModel
        direction = createOfferModel.direction

        quoteSidePaymentMethodDisplayString =
            createOfferModel.selectedQuoteSidePaymentMethods.joinToString(", ") {
                i18NPaymentMethod(it).first
            }
        baseSidePaymentMethodDisplayString =
            createOfferModel.selectedBaseSidePaymentMethods.joinToString(", ") {
                i18NPaymentMethod(it).first
            }

        // Fetch current market price to ensure all displayed values are consistent.
        // Market prices update every ~1 min via WebSocket, so the price stored in the model
        // from earlier wizard steps may be stale by the time the review screen is displayed.
        val currentMarketPrice = getCurrentMarketPrice()
        val effectivePrice = calculateEffectivePrice(currentMarketPrice)

        formatAmounts(effectivePrice)

        headLine = "${direction.name.uppercase()} Bitcoin"
        fee = "bisqEasy.tradeWizard.review.noTradeFees".i18n()
        feeDetails = "bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong".i18n()

        marketCodes = createOfferModel.market!!.marketCodes
        formattedPrice = PriceQuoteFormatter.format(effectivePrice, true, false)
        isRangeOffer = createOfferModel.amountType == CreateOfferCoordinator.AmountType.RANGE_AMOUNT

        applyPriceDetails(currentMarketPrice)
    }

    private fun getCurrentMarketPrice(): PriceQuoteVO? =
        try {
            createOfferModel.market?.let { createOfferCoordinator.getMostRecentPriceQuote(it) }
        } catch (e: Exception) {
            log.w(e) { "Could not fetch current market price, using stored values" }
            null
        }

    /**
     * For percentage-based pricing, recalculates the offer price from the current market price
     * and the stored percentage. For fixed pricing, returns the stored fixed price unchanged.
     */
    private fun calculateEffectivePrice(currentMarketPrice: PriceQuoteVO?): PriceQuoteVO {
        if (currentMarketPrice == null ||
            createOfferModel.priceType != CreateOfferCoordinator.PriceType.PERCENTAGE
        ) {
            return createOfferModel.priceQuote
        }
        return if (createOfferModel.percentagePriceValue != 0.0) {
            PriceUtil.fromMarketPriceMarkup(
                currentMarketPrice,
                createOfferModel.percentagePriceValue,
            )
        } else {
            currentMarketPrice
        }
    }

    /**
     * Formats amount strings, recalculating BTC amounts from the effective price
     * to ensure consistency with the displayed price.
     */
    private fun formatAmounts(effectivePrice: PriceQuoteVO) {
        val formattedQuoteAmount: String
        val formattedBaseAmount: String
        if (createOfferModel.amountType == CreateOfferCoordinator.AmountType.FIXED_AMOUNT) {
            formattedQuoteAmount =
                AmountFormatter.formatAmount(createOfferModel.quoteSideFixedAmount!!, true, true)
            // Recalculate BTC amount from the effective price for consistency
            val baseSideAmount =
                recalculateBaseSideAmount(
                    effectivePrice,
                    createOfferModel.quoteSideFixedAmount!!,
                ) ?: createOfferModel.baseSideFixedAmount!!
            formattedBaseAmount = AmountFormatter.formatAmount(baseSideAmount, false, false)
        } else {
            formattedQuoteAmount =
                AmountFormatter.formatRangeAmount(
                    createOfferModel.quoteSideMinRangeAmount!!,
                    createOfferModel.quoteSideMaxRangeAmount!!,
                    true,
                )
            val baseMin =
                recalculateBaseSideAmount(
                    effectivePrice,
                    createOfferModel.quoteSideMinRangeAmount!!,
                ) ?: createOfferModel.baseSideMinRangeAmount!!
            val baseMax =
                recalculateBaseSideAmount(
                    effectivePrice,
                    createOfferModel.quoteSideMaxRangeAmount!!,
                ) ?: createOfferModel.baseSideMaxRangeAmount!!
            formattedBaseAmount = AmountFormatter.formatRangeAmount(baseMin, baseMax, false)
            formattedBaseRangeMinAmount = AmountFormatter.formatAmount(baseMin, false, false)
            formattedBaseRangeMaxAmount = AmountFormatter.formatAmount(baseMax, false, false)
        }

        if (direction.isBuy) {
            amountToPay = formattedQuoteAmount
            amountToReceive = formattedBaseAmount
        } else {
            amountToPay = formattedBaseAmount
            amountToReceive = formattedQuoteAmount
        }
    }

    private fun recalculateBaseSideAmount(
        price: PriceQuoteVO,
        quoteSideAmount: FiatVO,
    ): CoinVO? =
        try {
            price.toBaseSideMonetary(
                FiatVOFactory.from(quoteSideAmount.value, quoteSideAmount.code),
            ) as CoinVO
        } catch (e: Exception) {
            log.w(e) { "Failed to recalculate base side amount" }
            null
        }

    private fun applyPriceDetails(currentMarketPrice: PriceQuoteVO?) {
        val marketPriceForDisplay =
            currentMarketPrice ?: createOfferModel.originalPriceQuote

        if (createOfferModel.priceType == CreateOfferCoordinator.PriceType.PERCENTAGE) {
            val percentage = createOfferModel.percentagePriceValue
            if (percentage == 0.0) {
                priceDetails = "bisqEasy.tradeWizard.review.priceDetails".i18n()
            } else {
                val priceWithCode =
                    PriceQuoteFormatter.format(marketPriceForDisplay, true, true)
                val percentagePrice = PercentageFormatter.format(abs(percentage), true)
                val aboveOrBelow =
                    if (percentage > 0) {
                        "mobile.general.above".i18n()
                    } else {
                        "mobile.general.below".i18n()
                    }
                priceDetails =
                    "bisqEasy.tradeWizard.review.priceDetails.float".i18n(
                        percentagePrice,
                        aboveOrBelow,
                        priceWithCode,
                    )
            }
        } else {
            // Fixed pricing: recalculate the percentage delta from current market price
            val percentage =
                if (currentMarketPrice != null) {
                    try {
                        PriceUtil.getPercentageToMarketPrice(
                            currentMarketPrice,
                            createOfferModel.priceQuote,
                        )
                    } catch (e: Exception) {
                        log.w(e) { "Failed to recalculate percentage for fixed price" }
                        createOfferModel.percentagePriceValue
                    }
                } else {
                    createOfferModel.percentagePriceValue
                }
            val priceWithCode =
                PriceQuoteFormatter.format(marketPriceForDisplay, true, true)
            if (percentage == 0.0) {
                priceDetails =
                    "bisqEasy.tradeWizard.review.priceDetails.fix.atMarket".i18n(priceWithCode)
            } else {
                val percentagePrice = PercentageFormatter.format(abs(percentage), true)
                val aboveOrBelow =
                    if (percentage > 0) {
                        "mobile.general.above".i18n()
                    } else {
                        "mobile.general.below".i18n()
                    }
                priceDetails =
                    "bisqEasy.tradeWizard.review.priceDetails.fix".i18n(
                        percentagePrice,
                        aboveOrBelow,
                        priceWithCode,
                    )
            }
        }
    }

    fun onBack() {
        navigateBack()
    }

    fun onClose() {
        navigateToOfferbookTab()
    }

    fun onCreateOffer() {
        if (createOfferCoordinator.isDemo) {
            showSnackbar("mobile.bisqEasy.offerbook.createOfferDisabledInDemonstrationMode".i18n(), type = SnackbarType.ERROR)
            return
        }
        _isCreateOfferBtnEnabled.value = false
        showLoading()
        presenterScope.launch {
            try {
                createOfferCoordinator
                    .createOffer()
                    .onSuccess {
                        navigateToOfferbookTab()
                    }.onFailure { exception ->
                        handleError(exception, defaultMessage = "mobile.bisqEasy.createOffer.failed".i18n()) { exception ->
                            val bannedError = exception.message?.contains("banned", ignoreCase = true) == true
                            if (bannedError) {
                                showSnackbar("mobile.bisqEasy.createOffer.userBanned".i18n(), type = SnackbarType.ERROR)
                                return@handleError true
                            } else {
                                return@handleError false
                            }
                        }
                        _isCreateOfferBtnEnabled.value = true
                    }
            } catch (e: Exception) {
                handleError(e, defaultMessage = "mobile.bisqEasy.createOffer.failed".i18n())
                _isCreateOfferBtnEnabled.value = true
            } finally {
                hideLoading()
            }
        }
    }
}
