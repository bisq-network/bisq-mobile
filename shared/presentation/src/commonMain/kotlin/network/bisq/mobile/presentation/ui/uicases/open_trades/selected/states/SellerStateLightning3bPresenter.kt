package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18nEncode
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

    override fun onViewAttached() {
        super.onViewAttached()
        val selectedTrade = tradesServiceFacade.selectedTrade.value!!
        val bisqEasyOpenTradeChannelModel = selectedTrade.bisqEasyOpenTradeChannelModel
        val peersUserName = bisqEasyOpenTradeChannelModel.getPeer().userName
        collectUI(bisqEasyOpenTradeChannelModel.chatMessages) { messages ->
            for (message in messages) {
                if (message.chatMessageType == ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE && message.textString.isNotEmpty()) {
                    val encodedLogMessage = message.textString
                    val encodedWithUserName = "bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln".i18nEncode(
                        peersUserName
                    )
                    val encodedWithNickName = getEncodedWithNickName(bisqEasyOpenTradeChannelModel)

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

    fun skipWaiting() {
        launchIO {
            tradesServiceFacade.btcConfirmed()
        }
    }

    fun completeTrade() {
        skipWaiting()
    }

    private fun getEncodedWithNickName(bisqEasyOpenTradeChannel: BisqEasyOpenTradeChannelModel): String {
        return "bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln".i18nEncode(
            bisqEasyOpenTradeChannel.getPeer().nickName
        )
    }
}