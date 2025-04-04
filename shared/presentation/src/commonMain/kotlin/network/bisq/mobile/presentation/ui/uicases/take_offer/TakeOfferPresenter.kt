package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.bitcoinFrom
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toQuoteSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class TakeOfferPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    class TakeOfferModel {
        lateinit var offerItemPresentationVO: OfferItemPresentationModel
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

    fun selectOfferToTake(value: OfferItemPresentationModel) {
        takeOfferModel = TakeOfferModel()
        takeOfferModel.offerItemPresentationVO = value

        val offerListItem = takeOfferModel.offerItemPresentationVO
        val bisqEasyOffer = offerListItem.bisqEasyOffer

        takeOfferModel.hasMultipleQuoteSidePaymentMethods = bisqEasyOffer.quoteSidePaymentMethodSpecs.size > 1
        takeOfferModel.hasMultipleBaseSidePaymentMethods = bisqEasyOffer.baseSidePaymentMethodSpecs.size > 1

        val amountSpec = bisqEasyOffer.amountSpec
        takeOfferModel.hasAmountRange =
            amountSpec is network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO

        val priceQuote: PriceQuoteVO = getMostRecentPriceQuote()
        takeOfferModel.priceQuote = priceQuote

        val quoteCurrencyCode = bisqEasyOffer.market.quoteCurrencyCode
        val baseCurrencyCode = bisqEasyOffer.market.baseCurrencyCode
        var quoteAmount = FiatVOFactory.from(0, quoteCurrencyCode)
        var baseAmount = CoinVOFactory.from(0, baseCurrencyCode)
        if (!takeOfferModel.hasAmountRange) {
            if (amountSpec is QuoteSideFixedAmountSpecVO) {
                quoteAmount = FiatVOFactory.from(amountSpec.amount, quoteCurrencyCode)
                baseAmount = priceQuote.toBaseSideMonetary(quoteAmount) as CoinVO
            } else if (amountSpec is BaseSideFixedAmountSpecVO) {
                baseAmount = CoinVOFactory.from(amountSpec.amount, baseCurrencyCode)
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

    fun commitAmount(
        priceQuote: PriceQuoteVO,
        quoteAmount: FiatVO,
        baseAmount: CoinVO
    ) {
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
        val result = tradesServiceFacade.takeOffer(
            takeOfferModel.offerItemPresentationVO.bisqEasyOffer,
            takeOfferModel.baseAmount,
            takeOfferModel.quoteAmount,
            takeOfferModel.baseSidePaymentMethod,
            takeOfferModel.quoteSidePaymentMethod,
            takeOfferStatus,
            takeOfferErrorMessage
        )
        if (result.isSuccess) {
            tradesServiceFacade.selectOpenTrade(result.getOrThrow())
        } else {
            // todo
            log.w { "Take offer failed ${result.exceptionOrNull()}" }
        }
    }

    fun getMostRecentPriceQuote(): PriceQuoteVO {
        val marketVO = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market
        val marketPriceItem: MarketPriceItem? = marketPriceServiceFacade.findMarketPriceItem(marketVO)
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        if (marketPriceItem != null) {
            return priceSpec.getPriceQuoteVO(marketPriceItem)
        } else {
            // FIXME happens in client mode. probably market price data are not received in time
            log.e { "marketPriceItem must not be null" }
            val marketPriceItem: MarketPriceItem? = marketPriceServiceFacade.findMarketPriceItem(marketVO)
            return PriceQuoteVO(
                0,
                4, 2,
                marketVO,
                CoinVOFactory.bitcoinFrom(1),
                FiatVOFactory.from(
                    marketPriceItem?.priceQuote?.value ?: 0L,
                    marketPriceItem?.market?.quoteCurrencyCode ?: "USD"
                )

            )
        }
    }
}

