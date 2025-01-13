package network.bisq.mobile.presentation.ui.uicases.create_offer

import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.account.payment_method.BitcoinPaymentRailEnum
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter


class CreateOfferPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade
) : BasePresenter(mainPresenter) {
    enum class PriceType {
        PERCENTAGE,
        FIXED,
    }

    enum class AmountType {
        FIXED_AMOUNT,
        RANGE_AMOUNT,
    }

    class CreateOfferModel {
        var market: MarketVO? = null
        var direction: DirectionEnum = DirectionEnum.BUY

        var amountType: AmountType = AmountType.FIXED_AMOUNT

        // FIXED_AMOUNT
        var fixedAmountSliderPosition = 0.5f
        var quoteSideFixedAmount: FiatVO? = null
        var baseSideFixedAmount: CoinVO? = null

        // RANGE_AMOUNT
        var rangeSliderPosition: ClosedFloatingPointRange<Float> = 0.0f..1.0f
        var quoteSideMinRangeAmount: FiatVO? = null
        var baseSideMinRangeAmount: CoinVO? = null
        var quoteSideMaxRangeAmount: FiatVO? = null
        var baseSideMaxRangeAmount: CoinVO? = null

        var fiatMinAmount: FiatVO? = null
        var fiatMaxAmount: FiatVO? = null

        var priceType: PriceType = PriceType.PERCENTAGE
        var percentagePriceValue: Double = 0.0
        lateinit var priceQuote: PriceQuoteVO

        lateinit var availableQuoteSidePaymentMethods: List<String>
        var availableBaseSidePaymentMethods: List<String> = BitcoinPaymentRailEnum.entries.map { it.name }
        var selectedQuoteSidePaymentMethods: Set<String> = emptySet()
        var selectedBaseSidePaymentMethods: Set<String> = emptySet()
    }

    lateinit var createOfferModel: CreateOfferModel

    fun onStartCreateOffer() {
        createOfferModel = CreateOfferModel()

        createOfferModel.apply {
            priceQuote = marketPriceServiceFacade.selectedMarketPriceItem.value!!.priceQuote
        }
    }

    fun commitDirection(value: DirectionEnum) {
        createOfferModel.direction = value
    }

    fun commitMarket(value: MarketVO) {
        createOfferModel.market = value
        createOfferModel.priceQuote = getMostRecentPriceQuote(value)
        createOfferModel.availableQuoteSidePaymentMethods = FiatPaymentRailUtil.getPaymentRailNames(value.quoteCurrencyCode)
    }

    fun commitAmount(
        amountType: AmountType,
        quoteSideFixedAmount: FiatVO,
        baseSideFixedAmount: CoinVO,
        quoteSideMinRangeAmount: FiatVO,
        baseSideMinRangeAmount: CoinVO,
        quoteSideMaxRangeAmount: FiatVO,
        baseSideMaxRangeAmount: CoinVO,
    ) {
        createOfferModel.amountType = amountType

        createOfferModel.quoteSideFixedAmount = quoteSideFixedAmount
        createOfferModel.baseSideFixedAmount = baseSideFixedAmount

        createOfferModel.quoteSideMinRangeAmount = quoteSideMinRangeAmount
        createOfferModel.baseSideMinRangeAmount = baseSideMinRangeAmount
        createOfferModel.quoteSideMaxRangeAmount = quoteSideMaxRangeAmount
        createOfferModel.baseSideMaxRangeAmount = baseSideMaxRangeAmount
    }

    fun commitPrice(priceType: PriceType, percentagePrice: Double, priceQuote: PriceQuoteVO) {
        createOfferModel.priceType = priceType
        createOfferModel.percentagePriceValue = percentagePrice
        createOfferModel.priceQuote = priceQuote
    }

    fun commitPaymentMethod(selectedQuoteSidePaymentMethods: Set<String>, selectedBaseSidePaymentMethods: Set<String>) {
        createOfferModel.selectedQuoteSidePaymentMethods = selectedQuoteSidePaymentMethods
        createOfferModel.selectedBaseSidePaymentMethods = selectedBaseSidePaymentMethods
    }

    suspend fun createOffer() {
        val direction: DirectionEnum = createOfferModel.direction
        val market: MarketVO = createOfferModel.market!!
        val bitcoinPaymentMethods: Set<String> = createOfferModel.selectedBaseSidePaymentMethods
        val fiatPaymentMethods: Set<String> = createOfferModel.selectedQuoteSidePaymentMethods

        var amountSpec: AmountSpecVO
        if (createOfferModel.amountType == AmountType.FIXED_AMOUNT) {
            amountSpec = QuoteSideFixedAmountSpecVO(createOfferModel.quoteSideFixedAmount!!.value)
        } else {
            amountSpec = QuoteSideRangeAmountSpecVO(
                createOfferModel.quoteSideMinRangeAmount!!.value,
                createOfferModel.quoteSideMaxRangeAmount!!.value
            )
        }
        val priceSpec: PriceSpecVO = if (createOfferModel.priceType == PriceType.FIXED) {
            FixPriceSpecVO(createOfferModel.priceQuote)
        } else {
            if (createOfferModel.percentagePriceValue == 0.0) MarketPriceSpecVO()
            else FloatPriceSpecVO(createOfferModel.percentagePriceValue)
        }

        val supportedLanguageCodes: Set<String> = setOf("en") //todo
        offersServiceFacade.createOffer(
            direction,
            market,
            bitcoinPaymentMethods,
            fiatPaymentMethods,
            amountSpec,
            priceSpec,
            supportedLanguageCodes
        )
    }

    fun getMostRecentPriceQuote(market: MarketVO): PriceQuoteVO {
        val marketPriceItem: MarketPriceItem = marketPriceServiceFacade.findMarketPriceItem(market)!!
        return MarketPriceSpecVO().getPriceQuoteVO(marketPriceItem)
    }
}
