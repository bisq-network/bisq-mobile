package network.bisq.mobile.client.offerbook

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.replicated_model.common.currency.MarketListItem
import network.bisq.mobile.client.service.Polling
import network.bisq.mobile.domain.client.main.user_profile.OfferbookApiGateway
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.offerbook.OfferbookListItem
import network.bisq.mobile.domain.offerbook.OfferbookMarket
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade

class ClientOfferbookServiceFacade(private val apiGateway: OfferbookApiGateway) :
    OfferbookServiceFacade {

    // Properties
    private val _marketListItems: MutableList<MarketListItem> = mutableListOf()
    override val marketListItemList: List<MarketListItem> get() = _marketListItems

    private val _offerbookListItems: MutableStateFlow<List<OfferbookListItem>> =
        MutableStateFlow(mutableListOf())
    override val offerbookListItemList: StateFlow<List<OfferbookListItem>> get() = _offerbookListItems

    private val _selectedOfferbookMarket: MutableStateFlow<OfferbookMarket> =
        MutableStateFlow(OfferbookMarket("", "", "", ""))
    override val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    // Misc
    private val log = Logger.withTag(this::class.simpleName ?: "ClientOfferbookServiceFacade")

    // TODO for dev testing we keep it short, later it should be maybe 5 sec. or we use websockets
    private var polling = Polling(1000) { getNumOffersByMarketCode() }

    // Life cycle
    override fun initialize() {
        CoroutineScope(BackgroundDispatcher).launch {
            val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()

            val list = apiGateway.getMarkets()
                .map { marketDto ->
                    val marketListItemWithNumOffers = MarketListItem(
                        marketDto.baseCurrencyCode,
                        marketDto.quoteCurrencyCode,
                        marketDto.baseCurrencyName,
                        marketDto.quoteCurrencyName,
                    )
                    val numOffers = numOffersByMarketCode[marketDto.quoteCurrencyCode] ?: 0
                    marketListItemWithNumOffers.setNumOffers(numOffers)
                    marketListItemWithNumOffers
                }
            _marketListItems.addAll(list)
        }

        polling.start()
    }

    override fun resume() {
        polling.start()
    }

    override fun dispose() {
        polling.stop()
    }

    override fun selectMarket(marketListItem: MarketListItem) {
        //todo
        log.i { "market " + marketListItem }
    }

    private fun getNumOffersByMarketCode() {
        CoroutineScope(BackgroundDispatcher).launch {
            val numOffersByMarketCode = apiGateway.getNumOffersByMarketCode()
            marketListItemList.map { marketListItem ->
                val numOffers = numOffersByMarketCode[marketListItem.quoteCurrencyCode] ?: 0
                marketListItem.setNumOffers(numOffers)
                marketListItem
            }
        }
    }
}