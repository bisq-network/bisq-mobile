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
    private var cachedCount: Int = 0

    init {
        resume()
    }

    fun resume() {
        log.d { "Resuming NumOffersObserver for channel: ${channel.id}, market: ${channel.market.marketCodes}" }
        dispose()
        channelPin = channel.chatMessages.addObserver {
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
        try {
            val count = channel.chatMessages.count { it.hasBisqEasyOffer() }

            // Only update if count changed to reduce unnecessary updates
            if (count != cachedCount) {
                cachedCount = count
                // Log only significant changes to reduce log spam
                if (count % 10 == 0 || count < 10) {
                    log.d { "Updated num offers for ${channel.market.marketCodes}: $count" }
                }
                setNumOffers(count)
            }
        } catch (e: Exception) {
            log.e(e) { "Error updating num offers for ${channel.market.marketCodes}" }
        }
    }
}