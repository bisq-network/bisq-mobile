package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class SellerStateLightning3bPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    private var _buyerHasConfirmedBitcoinReceipt: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var buyerHasConfirmedBitcoinReceipt: StateFlow<Boolean> = _buyerHasConfirmedBitcoinReceipt
    fun setBuyerHasConfirmedBitcoinReceipt(value: Boolean) {
        _buyerHasConfirmedBitcoinReceipt.value = value
    }

    private var job: Job? = null
    override fun onViewAttached() {
        super.onViewAttached()
        val selectedTrade = tradesServiceFacade.selectedTrade.value!!
        val bisqEasyOpenTradeChannelModel = selectedTrade.bisqEasyOpenTradeChannelModel
        val peersUserName = bisqEasyOpenTradeChannelModel.getPeer().userName
        presenterScope.launch {
            bisqEasyOpenTradeChannelModel.chatMessages.collect { messages ->
                for (message in messages) {
                    if (message.chatMessageType == ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE && message.textString.isNotEmpty()) {
                        val encodedLogMessage = message.textString
                        val encodedWithUserName = I18nSupport.encode(
                            "bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln",
                            peersUserName
                        )
                        val encodedWithNickName = getEncodedWithNickName(bisqEasyOpenTradeChannelModel);

                        if (encodedLogMessage.equals(encodedWithUserName) || encodedLogMessage.equals(
                                encodedWithNickName
                            )
                        ) {
                            _buyerHasConfirmedBitcoinReceipt.value = true
                        }
                    }
                }
            }
        }
    }

    override fun onViewUnattaching() {
        super.onViewUnattaching()
        job?.cancel()
        job = null
    }

    fun skipWaiting() {
        job = ioScope.launch {
            tradesServiceFacade.btcConfirmed()
        }
    }

    fun completeTrade() {
        job = ioScope.launch {
            tradesServiceFacade.btcConfirmed()
        }
    }

    private fun getEncodedWithNickName(bisqEasyOpenTradeChannel: BisqEasyOpenTradeChannelModel): String {
        return I18nSupport.encode(
            "bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln",
            bisqEasyOpenTradeChannel.getPeer().nickName
        )
    }
}