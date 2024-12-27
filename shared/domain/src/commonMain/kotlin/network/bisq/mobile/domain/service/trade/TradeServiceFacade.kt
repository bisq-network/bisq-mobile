package network.bisq.mobile.domain.service.trade

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.offer.bisq_easy.BisqEasyOfferVO

interface TradeServiceFacade : LifeCycleAware {
    suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ): Result<String>
}