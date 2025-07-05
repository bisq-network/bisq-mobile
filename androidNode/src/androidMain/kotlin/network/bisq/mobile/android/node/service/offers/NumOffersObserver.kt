package network.bisq.mobile.android.node.service.offers


import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.common.observable.Pin
import network.bisq.mobile.domain.utils.Logging

class NumOffersObserver(
    // TODO restore usage of bisqEasyOfferbookMessageService for v2.1.8
    private val channel: BisqEasyOfferbookChannel,
    val setNumOffers: (Int) -> Unit
) : Logging {
    private var channelPin: Pin? = null

    init {
        resume()
    }

    fun resume() {
        log.d { "Resuming NumOffersObserver for channel: ${channel.id}, market: ${channel.market.marketCodes}" }
        dispose()
        channelPin = channel.chatMessages.addObserver { 
            log.d { "Chat messages changed for ${channel.market.marketCodes}, updating num offers" }
            updateNumOffers() 
        }
        updateNumOffers() // Update immediately on resume
    }

    fun dispose() {
        log.d { "Disposing NumOffersObserver for channel: ${channel.id}" }
        channelPin?.unbind()
        channelPin = null
    }

    private fun updateNumOffers() {
//        TODO restore for core version v2.1.8
//        val count = bisqEasyOfferbookMessageService.getOffers(channel).count().toInt()
        val count = channel.chatMessages.stream()
            .filter { it.hasBisqEasyOffer() }
            .count().toInt()
        log.d { "Updated num offers for ${channel.market.marketCodes}: $count" }
        setNumOffers(count)
    }
}