package network.bisq.mobile.android.node.domain.offerbook.market

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.common.observable.Pin
import co.touchlab.kermit.Logger
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.client.replicated_model.common.currency.MarketListItem
import network.bisq.mobile.domain.LifeCycleAware


class MarketListItemService(private val applicationServiceSupplier: AndroidApplicationService.Supplier) :
    LifeCycleAware {
    // Properties
    private val _marketListItems: List<MarketListItem> by lazy { fillMarketListItems() }
    val marketListItems: List<MarketListItem> get() = _marketListItems

    // Misc
    private val log = Logger.withTag(this::class.simpleName ?: "Markets")
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()

    // Life cycle
    override fun initialize() {
    }

    override fun resume() {
        numOffersObservers.forEach { it.resume() }
    }

    override fun dispose() {
        numOffersObservers.forEach { it.dispose() }
    }

    private fun fillMarketListItems(): MutableList<MarketListItem> {
        val marketListItems: MutableList<MarketListItem> = mutableListOf()
        applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelService.channels
            .forEach { channel ->
                // We convert channel.market to our replicated Market model
                val marketListItem = MarketListItem(
                    channel.market.baseCurrencyCode,
                    channel.market.quoteCurrencyCode,
                    channel.market.baseCurrencyName,
                    channel.market.quoteCurrencyName,
                )
                marketListItems.add(marketListItem)

                val numOffersObserver = NumOffersObserver(channel, marketListItem::setNumOffers)
                numOffersObservers.add(numOffersObserver)
            }
        return marketListItems
    }

    // Inner class
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