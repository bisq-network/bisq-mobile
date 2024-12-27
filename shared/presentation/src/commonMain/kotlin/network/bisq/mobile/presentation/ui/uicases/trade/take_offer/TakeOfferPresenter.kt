package network.bisq.mobile.presentation.ui.uicases.trade.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.replicated.common.monetary.from
import network.bisq.mobile.domain.replicated.common.monetary.toBaseSideMonetary
import network.bisq.mobile.domain.replicated.common.monetary.toQuoteSideMonetary
import network.bisq.mobile.domain.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.replicated.offer.price.spec.getPriceQuoteVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.trade.TakeOfferStatus
import network.bisq.mobile.domain.service.trade.TradeServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class TakeOfferPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val tradeServiceFacade: TradeServiceFacade
) : BasePresenter(mainPresenter) {

    class TakeOfferModel {
        lateinit var offerListItem: OfferListItemVO
        var hasMultipleQuoteSidePaymentMethods: Boolean = false
        var hasMultipleBaseSidePaymentMethods: Boolean = false
        var hasAmountRange: Boolean = false
        lateinit var priceQuote: PriceQuoteVO
        lateinit var quoteAmount: FiatVO
        lateinit var baseAmount: CoinVO
        lateinit var quoteSidePaymentMethod: String
        lateinit var baseSidePaymentMethod: String
    }

    lateinit var takeOfferModel: TakeOfferModel

    fun selectOfferToTake(value: OfferListItemVO) {
        takeOfferModel = TakeOfferModel()
        takeOfferModel.offerListItem = value

        val offerListItem = takeOfferModel.offerListItem
        val bisqEasyOffer = offerListItem.bisqEasyOffer

        takeOfferModel.hasMultipleQuoteSidePaymentMethods = bisqEasyOffer.quoteSidePaymentMethodSpecs.size > 1
        takeOfferModel.hasMultipleBaseSidePaymentMethods = bisqEasyOffer.baseSidePaymentMethodSpecs.size > 1

        val amountSpec = bisqEasyOffer.amountSpec
        takeOfferModel.hasAmountRange = amountSpec is RangeAmountSpecVO

        val priceQuote: PriceQuoteVO = getMostRecentPriceQuote()
        takeOfferModel.priceQuote = priceQuote

        val quoteCurrencyCode = bisqEasyOffer.market.quoteCurrencyCode;
        val baseCurrencyCode = bisqEasyOffer.market.baseCurrencyCode;
        var quoteAmount = FiatVO.from(0, quoteCurrencyCode)
        var baseAmount = CoinVO.from(0, baseCurrencyCode)
        if (!takeOfferModel.hasAmountRange) {
            if (amountSpec is QuoteSideFixedAmountSpecVO) {
                quoteAmount = FiatVO.from(amountSpec.amount, quoteCurrencyCode)
                baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
            } else if (amountSpec is BaseSideFixedAmountSpecVO) {
                baseAmount = CoinVO.from(amountSpec.amount, baseCurrencyCode)
                quoteAmount = priceQuote.toQuoteSideMonetary(baseAmount) as FiatVO
            }
        }
        takeOfferModel.quoteAmount = quoteAmount
        takeOfferModel.baseAmount = baseAmount

        var quoteSidePaymentMethod = ""
        if (!takeOfferModel.hasMultipleQuoteSidePaymentMethods) {
            quoteSidePaymentMethod = offerListItem.quoteSidePaymentMethods[0]
        }
        takeOfferModel.quoteSidePaymentMethod = quoteSidePaymentMethod
        var baseSidePaymentMethod = ""
        if (!takeOfferModel.hasMultipleBaseSidePaymentMethods) {
            baseSidePaymentMethod = offerListItem.baseSidePaymentMethods[0]
        }
        takeOfferModel.baseSidePaymentMethod = baseSidePaymentMethod
    }

    fun showPaymentMethodsScreen(): Boolean {
        return takeOfferModel.hasMultipleQuoteSidePaymentMethods || takeOfferModel.hasMultipleBaseSidePaymentMethods
    }

    fun showAmountScreen(): Boolean {
        return takeOfferModel.hasAmountRange
    }

    fun commitAmount(priceQuote: PriceQuoteVO, quoteAmount: FiatVO, baseAmount: CoinVO) {
        takeOfferModel.priceQuote = priceQuote
        takeOfferModel.quoteAmount = quoteAmount
        takeOfferModel.baseAmount = baseAmount
    }


    fun commitPaymentMethod(quoteSidePaymentMethod: String, baseSidePaymentMethod: String) {
        takeOfferModel.quoteSidePaymentMethod = quoteSidePaymentMethod
        takeOfferModel.baseSidePaymentMethod = baseSidePaymentMethod
    }

    suspend fun takeOffer(
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ) {
        // todo add CompletableDeferred or callback to know when we have succeeded or failed
        // and get errors back
        tradeServiceFacade.takeOffer(
            takeOfferModel.offerListItem.bisqEasyOffer,
            takeOfferModel.baseAmount,
            takeOfferModel.quoteAmount,
            takeOfferModel.baseSidePaymentMethod,
            takeOfferModel.quoteSidePaymentMethod,
            takeOfferStatus,
            takeOfferErrorMessage
        )
    }

    fun getMostRecentPriceQuote(): PriceQuoteVO {
        val marketPriceItem: MarketPriceItem =
            marketPriceServiceFacade.findMarketPriceItem(takeOfferModel.offerListItem.bisqEasyOffer.market)!!
        val priceSpec = takeOfferModel.offerListItem.bisqEasyOffer.priceSpec
        return priceSpec.getPriceQuoteVO(marketPriceItem)
    }
}

