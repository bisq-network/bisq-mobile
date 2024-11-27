package network.bisq.mobile.android.node.domain.offerbook.market

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.currency.Market
import bisq.common.observable.Pin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.offerbook.NodeOfferbookServiceFacade.Companion.toReplicatedMarket
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.data.model.offerbook.market.OfferbookMarket
import network.bisq.mobile.utils.Logging


class NodeSelectedOfferbookMarketService(
    private val applicationService: AndroidApplicationService.Provider,
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) : LifeCycleAware, Logging {

    // Dependencies
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelService
    }
    private val bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelSelectionService
    }
    private val marketPriceService: MarketPriceService by lazy {
        applicationService.bondedRolesService.get().marketPriceService
    }

    // Properties
    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket.EMPTY)
    val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    // Misc
    private var selectedChannel: BisqEasyOfferbookChannel? = null
    private var selectedChannelPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Life cycle
    override fun activate() {
        observeSelectedChannel()
        observeMarketPrice()
    }

    override fun deactivate() {
        selectedChannelPin?.unbind()
        selectedChannelPin = null
        marketPricePin?.unbind()
        marketPricePin = null
    }

    // API
    fun selectMarket(market: Market) {
        log.i { "selectMarket " + market }
        bisqEasyOfferbookChannelService.findChannel(market).ifPresent {
            bisqEasyOfferbookChannelSelectionService.selectChannel(it)
        }
    }

    // Private
    private fun observeMarketPrice() {
        marketPricePin = marketPriceService.marketPriceByCurrencyMap.addObserver({
            marketPriceService.findMarketPriceQuote(marketPriceService.selectedMarket.get())
            updateMarketPrice()
        })
    }

    private fun observeSelectedChannel() {
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { selectedChannel ->
                this.selectedChannel = selectedChannel as BisqEasyOfferbookChannel
                marketPriceService.setSelectedMarket(selectedChannel.market)
                applySelectedOfferbookMarket()
            }
    }

    private fun applySelectedOfferbookMarket() {
        if (selectedChannel == null) {
            return
        }

        val selectedChannel = selectedChannel!!
        val market = toReplicatedMarket(selectedChannel.market)
        _selectedOfferbookMarket.value = OfferbookMarket(market)
        updateMarketPrice()
        log.i { _selectedOfferbookMarket.value.toString() }
    }

    private fun updateMarketPrice() {
        val formattedPrice = marketPriceServiceFacade.marketPriceItem.value.formattedPrice
        _selectedOfferbookMarket.value.setFormattedPrice(formattedPrice.value)
    }
}