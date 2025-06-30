package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.account.payment_method.BitcoinPaymentRailEnum
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.marketListDemoObj
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class CreateOfferPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter) {
    enum class PriceType {
        PERCENTAGE, FIXED,
    }

    enum class AmountType {
        FIXED_AMOUNT, RANGE_AMOUNT,
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
        var rangeSliderPosition: ClosedFloatingPointRange<Float> = 0.1f..0.9f
        var quoteSideMinRangeAmount: FiatVO? = null
        var baseSideMinRangeAmount: CoinVO? = null
        var quoteSideMaxRangeAmount: FiatVO? = null
        var baseSideMaxRangeAmount: CoinVO? = null

        var fiatMinAmount: FiatVO? = null
        var fiatMaxAmount: FiatVO? = null

        var priceType: PriceType = PriceType.PERCENTAGE
        var percentagePriceValue: Double = 0.0
        lateinit var originalPriceQuote: PriceQuoteVO
        lateinit var priceQuote: PriceQuoteVO

        lateinit var availableQuoteSidePaymentMethods: List<String>
        var availableBaseSidePaymentMethods: List<String> = BitcoinPaymentRailEnum.entries.map { it.name }
        var selectedQuoteSidePaymentMethods: Set<String> = emptySet()
        var selectedBaseSidePaymentMethods: Set<String> = emptySet()
    }

    lateinit var createOfferModel: CreateOfferModel

    var skipCurrency: Boolean = false

    fun onStartCreateOffer() {
        createOfferModel = CreateOfferModel()

        createOfferModel.apply {
            marketPriceServiceFacade.selectedMarketPriceItem.value?.let {
                originalPriceQuote = it.priceQuote
                priceQuote = it.priceQuote
            }
        }
    }

    fun commitDirection(value: DirectionEnum) {
        createOfferModel.direction = value
    }

    fun commitMarket(value: MarketVO) {
        createOfferModel.market = value
        val latestQuote = getMostRecentPriceQuote(value)
        createOfferModel.priceQuote = latestQuote
        createOfferModel.originalPriceQuote = latestQuote
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

        val quoteAmount = createOfferModel.quoteSideFixedAmount?.value
        val quoteCode = createOfferModel.quoteSideFixedAmount?.code

        if (quoteAmount != null && quoteCode != null && createOfferModel.baseSideFixedAmount != null) {
            createOfferModel.baseSideFixedAmount = priceQuote.toBaseSideMonetary(
                FiatVOFactory.from(
                    quoteAmount,
                    quoteCode
                )
            ) as CoinVO
        }

        val quoteMinRangeAmount = createOfferModel.quoteSideMinRangeAmount?.value

        if (quoteMinRangeAmount != null && quoteCode != null && createOfferModel.baseSideMinRangeAmount != null) {
            createOfferModel.baseSideMinRangeAmount = priceQuote.toBaseSideMonetary(
                FiatVOFactory.from(
                    quoteMinRangeAmount,
                    quoteCode
                )
            ) as CoinVO
        }

        val quoteMaxRangeAmount = createOfferModel.quoteSideMaxRangeAmount?.value

        if (quoteMaxRangeAmount != null && quoteCode != null && createOfferModel.baseSideMaxRangeAmount != null) {
            createOfferModel.baseSideMaxRangeAmount = priceQuote.toBaseSideMonetary(
                FiatVOFactory.from(
                    quoteMaxRangeAmount,
                    quoteCode
                )
            ) as CoinVO
        }
    }

    fun commitPaymentMethod(selectedQuoteSidePaymentMethods: Set<String>, selectedBaseSidePaymentMethods: Set<String>) {
        createOfferModel.selectedQuoteSidePaymentMethods = selectedQuoteSidePaymentMethods
        createOfferModel.selectedBaseSidePaymentMethods = selectedBaseSidePaymentMethods
    }

    suspend fun createOffer() {
        if (isDemo()) {
            showSnackbar("mobile.bisqEasy.offerbook.createOfferDisabledInDemonstrationMode".i18n())
            return
        }

        val direction: DirectionEnum = createOfferModel.direction
        val market: MarketVO = createOfferModel.market!!
        val bitcoinPaymentMethods: Set<String> = createOfferModel.selectedBaseSidePaymentMethods
        val fiatPaymentMethods: Set<String> = createOfferModel.selectedQuoteSidePaymentMethods

        val amountSpec = if (createOfferModel.amountType == AmountType.FIXED_AMOUNT) {
            QuoteSideFixedAmountSpecVO(createOfferModel.quoteSideFixedAmount!!.value)
        } else {
            QuoteSideRangeAmountSpecVO(
                createOfferModel.quoteSideMinRangeAmount!!.value, createOfferModel.quoteSideMaxRangeAmount!!.value
            )
        }
        val priceSpec = if (createOfferModel.priceType == PriceType.FIXED) {
            FixPriceSpecVO(createOfferModel.priceQuote)
        } else {
            if (createOfferModel.percentagePriceValue == 0.0) MarketPriceSpecVO()
            else FloatPriceSpecVO(createOfferModel.percentagePriceValue)
        }

        val supportedLanguageCodes = runCatching {
            settingsServiceFacade.getSettings().getOrThrow().supportedLanguageCodes
        }.getOrElse {
            log.w(it) { "Failed to fetch settings, defaulting to English" }
            setOf("en")
        }

        withContext(IODispatcher) {
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
    }

    fun getMostRecentPriceQuote(market: MarketVO): PriceQuoteVO {
        if (isDemo()) {
            val marketVO = marketListDemoObj.find { market.baseCurrencyCode == it.baseCurrencyCode && market.quoteCurrencyCode == market.quoteCurrencyCode }
            return PriceQuoteVO(
                100,
                4, 2,
                marketVO!!,
                CoinVO("BTC", 1, "BTC", 8, 4),
                FiatVO("USD", 80000, "USD", 4, 2),
            )
        } else {
            val marketPriceItem: MarketPriceItem? = marketPriceServiceFacade.findMarketPriceItem(market)
            if (marketPriceItem == null) {
                log.e { "Market price item not found for market: $market" }
                throw IllegalStateException("Market price not available for $market")
            }
            return MarketPriceSpecVO().getPriceQuoteVO(marketPriceItem)
        }
    }
}
