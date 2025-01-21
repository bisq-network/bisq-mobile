package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.TradeRoleEnumExtensions.isSeller
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BTC_CONFIRMED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.FAILED_AT_PEER
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.INIT
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_CANCELLED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.PEER_REJECTED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.REJECTED
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE1A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE1B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE2A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE2B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE3A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE_LIGHTNING3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE_MAIN_CHAIN3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE1
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE2A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE2B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE3A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE_LIGHTNING3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE_MAIN_CHAIN3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.*


class TradeFlowPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    val tradeStatesProvider: TradeStatesProvider
) : BasePresenter(mainPresenter) {

    val buyerState4Presenter = tradeStatesProvider.buyerState4Presenter
    val sellerState4Presenter = tradeStatesProvider.sellerState4Presenter

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade
    val steps = listOf(
        TradeFlowStep.ACCOUNT_DETAILS,
        TradeFlowStep.FIAT_PAYMENT,
        TradeFlowStep.BITCOIN_TRANSFER,
        TradeFlowStep.TRADE_COMPLETED
    )

    private var _tradePhaseState: MutableStateFlow<TradePhaseState> = MutableStateFlow(TradePhaseState.INIT)
    val tradePhaseState: StateFlow<TradePhaseState> = _tradePhaseState

    private var isSeller: Boolean = false
    private var isMainChain: Boolean = false

    override fun onViewAttached() {
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!

        isSeller = openTradeItemModel.bisqEasyTradeModel.tradeRole.isSeller
        val paymentMethod = openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
        isMainChain = paymentMethod == "MAIN_CHAIN"

        presenterScope.launch {
            openTradeItemModel.bisqEasyTradeModel.tradeState.collect { tradeState ->
                log.d { "Trade State Changed to: $tradeState" }
                tradeStateChanged(tradeState)
            }
        }
    }

    override fun onViewUnattaching() {
        _tradePhaseState.value = TradePhaseState.INIT
        isSeller = false
        isMainChain = false
        super.onViewUnattaching()
    }

    fun presenterForPhase(phase: TradePhaseState): BasePresenter? {
        return try {
            tradeStatesProvider.presenterForPhase(phase)
        } catch (e: IllegalArgumentException) {
            log.e { e.message.toString() }
            null
        }
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        if (state == null) {
            return
        }

        when (state) {
            INIT -> {}

            TAKER_SENT_TAKE_OFFER_REQUEST,

                // Seller
            MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,
            MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
                // Buyer
            MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> {
                if (isSeller) {
                    _tradePhaseState.value = SELLER_STATE1
                } else {
                    _tradePhaseState.value = BUYER_STATE1A
                }
            }
            // Seller
            MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS -> {
                _tradePhaseState.value = SELLER_STATE2A
            }

            SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> {
                _tradePhaseState.value = SELLER_STATE2B
            }

            SELLER_CONFIRMED_FIAT_RECEIPT -> _tradePhaseState.value = SELLER_STATE3A

            SELLER_SENT_BTC_SENT_CONFIRMATION ->
                if (isMainChain) {
                    _tradePhaseState.value = SELLER_STATE_MAIN_CHAIN3B
                } else {
                    _tradePhaseState.value = SELLER_STATE_LIGHTNING3B
                }

            // Buyer
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_,
            TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                _tradePhaseState.value = BUYER_STATE1B

            TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA ->
                _tradePhaseState.value = BUYER_STATE2A

            BUYER_SENT_FIAT_SENT_CONFIRMATION ->
                _tradePhaseState.value = BUYER_STATE2B

            BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION ->
                _tradePhaseState.value = BUYER_STATE3A

            BUYER_RECEIVED_BTC_SENT_CONFIRMATION ->
                if (isMainChain) {
                    _tradePhaseState.value = BUYER_STATE_MAIN_CHAIN3B
                } else {
                    _tradePhaseState.value = BUYER_STATE_LIGHTNING3B
                }

            BTC_CONFIRMED -> {
                if (isSeller) {
                    _tradePhaseState.value = SELLER_STATE4
                } else {
                    _tradePhaseState.value = BUYER_STATE4
                }
            }

            REJECTED, PEER_REJECTED -> {}
            CANCELLED, PEER_CANCELLED -> {}
            FAILED -> {}
            FAILED_AT_PEER -> {}
        }
    }

    enum class TradeFlowStep {
        ACCOUNT_DETAILS,
        FIAT_PAYMENT,
        BITCOIN_TRANSFER,
        TRADE_COMPLETED
    }

    enum class TradePhaseState(val index: Int) {
        INIT(0),
        SELLER_STATE1(0),
        SELLER_STATE2A(1),
        SELLER_STATE2B(1),
        SELLER_STATE3A(2),
        SELLER_STATE_MAIN_CHAIN3B(2),
        SELLER_STATE_LIGHTNING3B(2),
        SELLER_STATE4(3),

        BUYER_STATE1A(0),
        BUYER_STATE1B(0),
        BUYER_STATE2A(1),
        BUYER_STATE2B(1),
        BUYER_STATE3A(2),
        BUYER_STATE_MAIN_CHAIN3B(2),
        BUYER_STATE_LIGHTNING3B(2),
        BUYER_STATE4(3)
    }
}
