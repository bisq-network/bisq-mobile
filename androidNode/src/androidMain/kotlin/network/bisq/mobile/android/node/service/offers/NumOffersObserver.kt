package network.bisq.mobile.android.node.service.offers

import bisq.bisq_easy.BisqEasyOfferbookMessageService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.common.observable.Pin

class NumOffersObserver(
    private val bisqEasyOfferbookMessageService: BisqEasyOfferbookMessageService,
    private val channel: BisqEasyOfferbookChannel,
    val setNumOffers: (Int) -> Unit
) {
    private var channelPin: Pin? = null

    init {
        resume()
    }

    fun resume() {
        dispose()
        channelPin = channel.chatMessages.addObserver { updateNumOffers() }
    }

    fun dispose() {
        channelPin?.unbind()
        channelPin = null
    }

    private fun updateNumOffers() {
        setNumOffers(bisqEasyOfferbookMessageService.getOffers(channel).count().toInt())
    }
}