package network.bisq.mobile.data.service.trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.LifeCycleAware

interface TradesServiceFacade : LifeCycleAware {
    val selectedTrade: StateFlow<TradeItemPresentationModel?>
    val openTradeItems: StateFlow<List<TradeItemPresentationModel>>

    suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>,
    ): Result<String>

    fun selectOpenTrade(tradeId: String)

    suspend fun rejectTrade(): Result<Unit>

    suspend fun cancelTrade(): Result<Unit>

    suspend fun closeTrade(): Result<Unit>

    suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit>

    suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit>

    suspend fun sellerConfirmFiatReceipt(): Result<Unit>

    suspend fun buyerConfirmFiatSent(): Result<Unit>

    suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit>

    suspend fun btcConfirmed(): Result<Unit>

    suspend fun exportTradeDate(): Result<Unit>

    fun resetSelectedTradeToNull()
}
