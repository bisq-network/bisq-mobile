package network.bisq.mobile.data.service.trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

interface TradesServiceFacade : LifeCycleAware {
    val selectedTrade: StateFlow<TradeItemPresentationModel?>
    val openTradeItems: StateFlow<List<TradeItemPresentationModel>>

    /**
     * Change signal for closed trades. Increments whenever the server pushes a closed-trades update
     * or the local closed-trades collection mutates. Consumers should use this to trigger re-fetching
     * paginated closed-trade history.
     */
    val closedTradesChangeTick: StateFlow<Int>

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

    suspend fun getClosedTradesPaginated(
        params: PaginationParams,
        search: String? = null,
        sortBy: TradeSort? = null,
        outcomeFilter: TradeOutcomeFilter = TradeOutcomeFilter.ALL,
        roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
    ): Result<PaginatedResponse<ClosedTradeListItem>>
}
