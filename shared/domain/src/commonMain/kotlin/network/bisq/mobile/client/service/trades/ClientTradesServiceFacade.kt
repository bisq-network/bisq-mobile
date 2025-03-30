package network.bisq.mobile.client.service.trades

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Subscription
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientTradesServiceFacade(
    private val apiGateway: TradesApiGateway,
    webSocketClientProvider: WebSocketClientProvider,
    json: Json
) :
    TradesServiceFacade, Logging {

    // Properties
    private val _openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = _openTradeItems

    private val _selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)
    override val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = _selectedTrade

    // Misc
    private val tradeId get() = selectedTrade.value?.tradeId
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private val openTradesSubscription: Subscription<TradeItemPresentationDto> =
        Subscription(webSocketClientProvider, json, Topic.TRADES, this::handleTradeItemPresentationChange)

    private val tradePropertiesSubscription: Subscription<Map<String, TradePropertiesDto>> =
        Subscription(webSocketClientProvider, json, Topic.TRADE_PROPERTIES, this::handleTradePropertiesChange)

    //private var openTradesSubscriptionJob: Job? = null
    // private var tradePropertiesSubscriptionJob: Job? = null

    override fun activate() {
        openTradesSubscription.subscribe()
        tradePropertiesSubscription.subscribe()
    }

    override fun deactivate() {
        openTradesSubscription.dispose()
        tradePropertiesSubscription.dispose()

        /*        openTradesSubscriptionJob?.cancel()
                openTradesSubscriptionJob = null

                tradePropertiesSubscriptionJob?.cancel()
                tradePropertiesSubscriptionJob = null*/
    }

    // API
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

    override fun selectOpenTrade(tradeId: String) {
        _selectedTrade.value = findOpenTradeItemModel(tradeId)
    }

    override suspend fun rejectTrade(): Result<Unit> {
        return apiGateway.rejectTrade(requireNotNull(tradeId))
    }

    override suspend fun cancelTrade(): Result<Unit> {
        return apiGateway.cancelTrade(requireNotNull(tradeId))
    }

    override suspend fun closeTrade(): Result<Unit> {
        val result = apiGateway.closeTrade(requireNotNull(tradeId))
        if (result.isSuccess) {
            _selectedTrade.value = null
        }
        return result
    }

    override suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit> {
        return apiGateway.sellerSendsPaymentAccount(requireNotNull(tradeId), paymentAccountData)
    }

    override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit> {
        return apiGateway.buyerSendBitcoinPaymentData(requireNotNull(tradeId), bitcoinPaymentData)
    }

    override suspend fun sellerConfirmFiatReceipt(): Result<Unit> {
        return apiGateway.sellerConfirmFiatReceipt(requireNotNull(tradeId))
    }

    override suspend fun buyerConfirmFiatSent(): Result<Unit> {
        return apiGateway.buyerConfirmFiatSent(requireNotNull(tradeId))
    }

    override suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit> {
        return apiGateway.sellerConfirmBtcSent(requireNotNull(tradeId), paymentProof)
    }

    override suspend fun btcConfirmed(): Result<Unit> {
        return apiGateway.btcConfirmed(requireNotNull(tradeId))
    }

    override suspend fun exportTradeDate(): Result<Unit> {
        //todo
        return Result.success(Unit)
    }

    // Private
    private fun handleTradeItemPresentationChange(payload: List<TradeItemPresentationDto>, modificationType: ModificationType) {
        if (modificationType == ModificationType.REPLACE ||
            modificationType == ModificationType.ADDED
        ) {
            payload.forEach { item ->
                _openTradeItems.value += TradeItemPresentationModel(item)
            }
        } else if (modificationType == ModificationType.REMOVED) {
            payload.forEach { item ->
                val toRemove: TradeItemPresentationModel? = findOpenTradeItemModel(item.trade.id)
                if (toRemove != null) {
                    _openTradeItems.value -= toRemove
                }
            }
        }
        //applyOffersToSelectedMarket()
    }

    private fun handleTradePropertiesChange(payload: List<Map<String, TradePropertiesDto>>, modificationType: ModificationType) {
        payload.flatMap { it.entries }
            .forEach { (tradeId, data) ->
                findOpenTradeItemModel(tradeId)?.apply {
                    log.i { "Apply mutable data to trade with ID $tradeId" }
                    data.tradeState?.let { bisqEasyTradeModel.tradeState.value = it }
                    data.interruptTradeInitiator?.let { bisqEasyTradeModel.interruptTradeInitiator.value = it }
                    data.paymentAccountData?.let { bisqEasyTradeModel.paymentAccountData.value = it }
                    data.bitcoinPaymentData?.let { bisqEasyTradeModel.bitcoinPaymentData.value = it }
                    data.paymentProof?.let { bisqEasyTradeModel.paymentProof.value = it }
                    data.errorMessage?.let { bisqEasyTradeModel.errorMessage.value = it }
                    data.errorStackTrace?.let { bisqEasyTradeModel.errorStackTrace.value = it }
                    data.peersErrorMessage?.let { bisqEasyTradeModel.peersErrorMessage.value = it }
                    data.peersErrorStackTrace?.let { bisqEasyTradeModel.peersErrorStackTrace.value = it }
                }
            }
    }

    private fun findOpenTradeItemModel(tradeId: String): TradeItemPresentationModel? =
        _openTradeItems.value.find { it.tradeId == tradeId }

}