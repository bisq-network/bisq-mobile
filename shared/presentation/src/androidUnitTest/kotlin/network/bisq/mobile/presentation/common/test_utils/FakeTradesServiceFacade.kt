package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TakeOfferStatus
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

/**
 * No-op [TradesServiceFacade] for presenters that need one injected but do not exercise it. Only
 * [takeOffer] varies between call sites, so it is a constructor parameter; override anything else in
 * a subclass rather than growing this one.
 */
internal open class FakeTradesServiceFacade(
    private val takeOfferResult: Result<String> = Result.success("trade-1"),
) : TradesServiceFacade {
    override val selectedTrade: StateFlow<TradeItemPresentationModel?> = MutableStateFlow(null)
    override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(emptyList())
    override val closedTradesChangeTick: StateFlow<Int> = MutableStateFlow(0)
    override val openTradesSynced: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun getClosedTradesPaginated(
        params: PaginationParams,
        search: String?,
        sortBy: TradeSort?,
        outcomeFilter: TradeOutcomeFilter,
        roleFilter: TradeRoleFilter,
    ): Result<PaginatedResponse<ClosedTradeListItem>> = Result.success(PaginatedResponse(emptyList(), params.page, params.pageSize, 0L, 0))

    override suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>,
    ): Result<String> = takeOfferResult

    override fun selectOpenTrade(tradeId: String) {}

    override suspend fun rejectTrade(reason: AnalyticsEvent.Trade.InterruptReason): Result<Unit> = Result.success(Unit)

    override suspend fun cancelTrade(reason: AnalyticsEvent.Trade.InterruptReason): Result<Unit> = Result.success(Unit)

    override suspend fun closeTrade(): Result<Unit> = Result.success(Unit)

    override suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit> = Result.success(Unit)

    override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit> = Result.success(Unit)

    override suspend fun sellerConfirmFiatReceipt(): Result<Unit> = Result.success(Unit)

    override suspend fun buyerConfirmFiatSent(): Result<Unit> = Result.success(Unit)

    override suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit> = Result.success(Unit)

    override suspend fun btcConfirmed(): Result<Unit> = Result.success(Unit)

    override suspend fun exportTradeDate(): Result<Unit> = Result.success(Unit)

    override fun resetSelectedTradeToNull() {}
}
