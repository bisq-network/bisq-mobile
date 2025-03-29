package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.contract.RoleEnum
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class InterruptedTradePresenter(
    mainPresenter: MainPresenter,
    private var tradesServiceFacade: TradesServiceFacade,
    private var mediationServiceFacade: MediationServiceFacade
) : BasePresenter(mainPresenter) {

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private var _interruptedTradeInfo: MutableStateFlow<String> = MutableStateFlow("")
    val interruptedTradeInfo: StateFlow<String> = _interruptedTradeInfo

    private var _interruptionInfoVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val interruptionInfoVisible: StateFlow<Boolean> = _interruptionInfoVisible

    var errorMessage: String = ""
    private var _errorMessageVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val errorMessageVisible: StateFlow<Boolean> = _errorMessageVisible

    private val _isInMediation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInMediation: StateFlow<Boolean> = _isInMediation

    private val _reportToMediatorButtonVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val reportToMediatorButtonVisible: StateFlow<Boolean> = _reportToMediatorButtonVisible

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        presenterScope.launch {
            openTradeItemModel.bisqEasyTradeModel.tradeState.collect { tradeState ->
                tradeStateChanged(tradeState)
            }
        }
        presenterScope.launch {
            openTradeItemModel.bisqEasyOpenTradeChannelModel.isInMediation.collect { isInMediation ->
                _isInMediation.value = isInMediation
            }
        }
    }

    override fun onViewUnattaching() {
        reset()
        super.onViewUnattaching()
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        reset()

        if (state == null) {
            return
        }

        when (state) {
            BisqEasyTradeStateEnum.INIT,
            BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> {
            }

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,

            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT,
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
            }

            BisqEasyTradeStateEnum.BTC_CONFIRMED -> {
            }

            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED -> {
                _interruptionInfoVisible.value = true

                val wasTradeCancelled = state == BisqEasyTradeStateEnum.CANCELLED ||
                        state == BisqEasyTradeStateEnum.PEER_CANCELLED
                val trade = selectedTrade.value!!.bisqEasyTradeModel
                val isMaker: Boolean = trade.isMaker
                val makerInterruptedTrade =
                    trade.interruptTradeInitiator.value == RoleEnum.MAKER
                val selfInitiated = (makerInterruptedTrade && isMaker) || (!makerInterruptedTrade && !isMaker)

                if (wasTradeCancelled) {
                    _interruptedTradeInfo.value =
                        if (selfInitiated) "bisqEasy.openTrades.cancelled.self".i18n() else "bisqEasy.openTrades.cancelled.peer".i18n()
                    _reportToMediatorButtonVisible.value = !selfInitiated
                } else {
                    _interruptedTradeInfo.value =
                        if (selfInitiated) "bisqEasy.openTrades.rejected.self".i18n() else "bisqEasy.openTrades.rejected.peer".i18n()
                }
            }

            BisqEasyTradeStateEnum.FAILED -> {
                _reportToMediatorButtonVisible.value = false
                errorMessage =
                    "The trade failed with error message: ${selectedTrade.value?.bisqEasyTradeModel?.errorMessage?.value}" // bisqEasy.openTrades.failed
                _errorMessageVisible.value = true
            }

            BisqEasyTradeStateEnum.FAILED_AT_PEER -> {
                _reportToMediatorButtonVisible.value = false
                errorMessage =
                    "The peer's trade failed with an error caused by: ${selectedTrade.value?.bisqEasyTradeModel?.peersErrorMessage?.value}" // bisqEasy.openTrades.failedAtPeer
                _errorMessageVisible.value = true
            }
        }
    }

    fun onCloseTrade() {
        backgroundScope.launch {
            if (selectedTrade.value != null) {
                tradesServiceFacade.closeTrade()
            }
            navigateBack()
        }
    }

    fun onReportToMediator() {
        backgroundScope.launch {
            mediationServiceFacade.reportToMediator(selectedTrade.value!!)
        }
    }

    private fun reset() {
        _interruptedTradeInfo.value = ""
        _interruptionInfoVisible.value = false
        errorMessage = ""
        _errorMessageVisible.value = false
        _reportToMediatorButtonVisible.value = false
    }
}

