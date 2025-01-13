package network.bisq.mobile.android.node.service.offers

import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.common.observable.Pin

class NumOffersObserver(private val channel: BisqEasyOfferbookChannel, val setNumOffers: (Int) -> Unit) {
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
        val numOffers = channel.chatMessages.stream()
            .filter { obj: BisqEasyOfferbookMessage -> obj.hasBisqEasyOffer() }
            .count().toInt()
        setNumOffers(numOffers)
    }
}