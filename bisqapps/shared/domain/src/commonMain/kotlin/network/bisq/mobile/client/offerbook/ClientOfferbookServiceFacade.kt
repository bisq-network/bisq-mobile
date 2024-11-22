package network.bisq.mobile.client.offerbook

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.client.service.Polling
import network.bisq.mobile.domain.client.main.user_profile.OfferbookApiGateway
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade

class ClientOfferbookServiceFacade(private val apiGateway: OfferbookApiGateway) :
    OfferbookServiceFacade {

    private val log = Logger.withTag(this::class.simpleName ?: "ClientOfferbookServiceFacade")
    private val _markets: MutableList<Market> = mutableListOf()
    override val markets: List<Market> get() = _markets

    // TODO for dev testing we keep it short, later it should be maybe 5 sec. or we use websockets
    private var polling = Polling(1000) { getNumOffersByMarketCode() }

    override fun initialize() {
        CoroutineScope(BackgroundDispatcher).launch {
            val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()

            val list = apiGateway.getMarkets()
                .map { marketDto ->
                    val marketWithNumOffers = Market(
                        marketDto.baseCurrencyCode,
                        marketDto.quoteCurrencyCode,
                        marketDto.baseCurrencyName,
                        marketDto.quoteCurrencyName,
                    )
                    val numOffers = numOffersByMarketCode[marketDto.quoteCurrencyCode] ?: 0
                    marketWithNumOffers.setNumOffers(numOffers)
                    marketWithNumOffers
                }
            _markets.addAll(list)
        }

        polling.start()
    }

    private fun getNumOffersByMarketCode() {
        CoroutineScope(BackgroundDispatcher).launch {
            val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()
            markets.map { marketListItem ->
                val numOffers = numOffersByMarketCode[marketListItem.quoteCurrencyCode] ?: 0
                marketListItem.setNumOffers(numOffers)
                marketListItem
            }
        }
    }

    override fun resume() {
        polling.start()
    }

    override fun dispose() {
        polling.stop()
    }
}