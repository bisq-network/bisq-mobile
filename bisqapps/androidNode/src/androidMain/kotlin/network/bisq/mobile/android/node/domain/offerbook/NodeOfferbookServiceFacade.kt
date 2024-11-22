package network.bisq.mobile.android.node.domain.offerbook

import bisq.chat.ChatService
import co.touchlab.kermit.Logger
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade

/**
 * This is a facade to the Bisq 2 libraries UserIdentityService and UserProfileServices.
 * It provides the API for the users profile presenter to interact with that domain.
 * It uses in a in-memory model for the relevant data required for the presenter to reflect the domains state.
 * Persistence is done inside the Bisq 2 libraries.
 */
class NodeOfferbookServiceFacade(
    private val applicationServiceSupplier: AndroidApplicationService.Supplier
) :
    OfferbookServiceFacade {

    private val log = Logger.withTag(this::class.simpleName ?: "NodeOfferbookServiceFacade")
    private val _marketWithNumOffers: List<Market> by lazy { fillMarketListItems() }
    override val markets: List<Market> get() = _marketWithNumOffers
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()

    private fun fillMarketListItems(): MutableList<Market> {
        val markets: MutableList<Market> = mutableListOf()
        chatService.bisqEasyOfferbookChannelService.channels
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

    private val chatService: ChatService
        get() = applicationServiceSupplier.chatServiceSupplier.get()


    override fun initialize() {
    }

    override fun dispose() {
        numOffersObservers.forEach { it.dispose() }
    }

    override fun resume() {
        numOffersObservers.forEach { it.resume() }
    }
}