package network.bisq.mobile.client.service.trade

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.service.trade.TakeOfferStatus
import network.bisq.mobile.domain.service.trade.TradeServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientTradeServiceFacade(
    private val apiGateway: TradeApiGateway,
) :
    TradeServiceFacade, Logging {

    override suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ): Result<String> {
        val apiResult = apiGateway.takeOffer(
            bisqEasyOffer.id,
            takersBaseSideAmount.value,
            takersQuoteSideAmount.value,
            bitcoinPaymentMethod,
            fiatPaymentMethod,
        )
        if (apiResult.isSuccess) {
            takeOfferStatus.value = TakeOfferStatus.SUCCESS
            return Result.success(apiResult.getOrThrow().tradeId)
        } else {
            throw apiResult.exceptionOrNull()!!
        }
    }
}