package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes


class TradeDetailsHeaderPresenter(
    mainPresenter: MainPresenter,
    var tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    enum class TradeCloseType {
        REJECT,
        CANCEL,
        COMPLETED
    }

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    var direction: String = ""
    var leftAmountDescription: String = ""
    var leftAmount: String = ""
    var leftCode: String = ""
    var rightAmountDescription: String = ""
    var rightAmount: String = ""
    var rightCode: String = ""

    private var tradeCloseType: TradeCloseType? = null

    private var _interruptTradeButtonText: MutableStateFlow<String> = MutableStateFlow("")
    val interruptTradeButtonText: StateFlow<String> = _interruptTradeButtonText

    private var _interruptTradeButtonVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val interruptTradeButtonVisible: StateFlow<Boolean> = _interruptTradeButtonVisible

    override fun onViewAttached() {
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!

        if (openTradeItemModel.bisqEasyTradeModel.isSeller) {
            direction = "SELL" //"offer.sell"
            leftAmountDescription = "Amount to send" //"bisqEasy.tradeState.header.send"
            leftAmount = openTradeItemModel.formattedBaseAmount
            leftCode = openTradeItemModel.baseCurrencyCode
            rightAmountDescription = "Amount to receive" // "bisqEasy.tradeState.header.receive"
            rightAmount = openTradeItemModel.formattedQuoteAmount
            rightCode = openTradeItemModel.quoteCurrencyCode
        } else {
            direction = "BUY" //"offer.sell"
            leftAmountDescription = "Amount to pay" //"bisqEasy.tradeState.header.pay"
            leftAmount = openTradeItemModel.formattedQuoteAmount
            leftCode = openTradeItemModel.quoteCurrencyCode
            rightAmountDescription = "Amount to receive" //"bisqEasy.tradeState.header.receive"
            rightAmount = openTradeItemModel.formattedBaseAmount
            rightCode = openTradeItemModel.baseCurrencyCode
        }

        presenterScope.launch {
            openTradeItemModel.bisqEasyTradeModel.tradeState.collect { tradeState ->
                tradeStateChanged(tradeState)
            }
        }
    }

    override fun onViewUnattaching() {
        reset()
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        tradeCloseType = null
        _interruptTradeButtonText.value = ""
        _interruptTradeButtonVisible.value = false

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
                _interruptTradeButtonVisible.value = true
                tradeCloseType = TradeCloseType.REJECT
                _interruptTradeButtonText.value = "Reject trade" // bisqEasy.openTrades.rejectTrade
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
                _interruptTradeButtonVisible.value = true
                tradeCloseType = TradeCloseType.CANCEL
                _interruptTradeButtonText.value = "bisqEasy.openTrades.cancelTrade".i18n()
            }

            BisqEasyTradeStateEnum.BTC_CONFIRMED -> {
                _interruptTradeButtonVisible.value = false
                tradeCloseType = TradeCloseType.COMPLETED
                _interruptTradeButtonText.value = ""
            }

            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED -> {
                _interruptTradeButtonVisible.value = false
            }

            BisqEasyTradeStateEnum.FAILED -> {
                _interruptTradeButtonVisible.value = false
            }

            BisqEasyTradeStateEnum.FAILED_AT_PEER -> {
                _interruptTradeButtonVisible.value = false
            }
        }
    }

    fun onInterruptTrade() {
        //todo who warning to user if he really want to reject. See onInterruptTrade in Bisq 2 TradeStateController
        backgroundScope.launch {
            require(selectedTrade.value != null)

            var result: Result<Unit>? = null
            if (tradeCloseType == TradeCloseType.REJECT) {
                result = tradesServiceFacade.rejectTrade()
            } else if (tradeCloseType == TradeCloseType.CANCEL) {
                result = tradesServiceFacade.cancelTrade()
            }
            if (result != null) {
                when {
                    // TODO review
                    result.isFailure -> closeWorkflow()
                    result.isSuccess -> closeWorkflow()
                }
            }
        }
    }

    private fun closeWorkflow() {
//        Do not navigate, close button on the same screen does it
//        navigateBack()
    }

    private fun reset() {
        direction = ""
        leftAmountDescription = ""
        leftAmount = ""
        leftCode = ""
        rightAmountDescription = ""
        rightAmount = ""
        rightCode = ""

        tradeCloseType = null
        _interruptTradeButtonText.value = ""
        _interruptTradeButtonVisible.value = false
    }
}
