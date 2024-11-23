package network.bisq.mobile.android.node.domain.offerbook.market

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.common.observable.Pin
import co.touchlab.kermit.Logger
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.client.replicated_model.common.currency.Market


class Markets(private val applicationServiceSupplier: AndroidApplicationService.Supplier) {

    private val log = Logger.withTag(this::class.simpleName ?: "NodeOfferbookServiceFacade")
    private val _markets: List<Market> by lazy { fillMarketListItems() }
    val markets: List<Market> get() = _markets
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()

    fun initialize() {
    }

    fun resume() {
        numOffersObservers.forEach { it.resume() }
    }

    fun dispose() {
        numOffersObservers.forEach { it.dispose() }
    }

    private fun fillMarketListItems(): MutableList<Market> {
        val markets: MutableList<Market> = mutableListOf()
        applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelService.channels
            .forEach { channel ->
                val _market = channel.market // Bisq 2 domain object
                // We convert to our replicated Market model
                val market = Market(
                    _market.baseCurrencyCode,
                    _market.quoteCurrencyCode,
                    _market.baseCurrencyName,
                    _market.quoteCurrencyName,
                )
                markets.add(market)

                val numOffersObserver = NumOffersObserver(channel, market::setNumOffers)
                numOffersObservers.add(numOffersObserver)
            }
        return markets
    }

    inner class NumOffersObserver(
        private val channel: BisqEasyOfferbookChannel,
        val setNumOffers: (Int) -> Unit
    ) {
        private var channelPin: Pin? = null

        init {
            channelPin = channel.chatMessages.addObserver { this.updateNumOffers() }
        }

        fun resume() {
            dispose()
            channelPin = channel.chatMessages.addObserver { this.updateNumOffers() }
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

}