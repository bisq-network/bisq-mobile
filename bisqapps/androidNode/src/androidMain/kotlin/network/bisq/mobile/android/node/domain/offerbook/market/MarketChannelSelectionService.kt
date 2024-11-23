package network.bisq.mobile.android.node.domain.offerbook.market

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.currency.Market
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.offerbook.OfferbookMarket


class MarketChannelSelectionService(private val applicationServiceSupplier: AndroidApplicationService.Supplier) :
    LifeCycleAware {

    // Dependencies
    private lateinit var bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService
    private lateinit var bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService
    private lateinit var marketPriceService: MarketPriceService

    // Properties
    private val _selectedOfferbookMarket = MutableStateFlow(OfferbookMarket("", "", "", ""))
    val selectedOfferbookMarket: StateFlow<OfferbookMarket> get() = _selectedOfferbookMarket

    // Misc
    private val log = Logger.withTag(this::class.simpleName ?: "SelectedMarket")

    private var selectedChannelPin: Pin? = null

    // Life cycle
    override fun initialize() {
        bisqEasyOfferbookChannelService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelService
        bisqEasyOfferbookChannelSelectionService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelSelectionService
        marketPriceService =
            applicationServiceSupplier.bondedRolesServiceSupplier.get().marketPriceService

        observeSelectedChannel()
    }

    override fun resume() {
        observeSelectedChannel()
    }

    override fun dispose() {
        selectedChannelPin?.unbind()
        selectedChannelPin = null
    }

    // API
    fun selectMarket(market: Market) {
        log.i { "selectMarket " + market }
        bisqEasyOfferbookChannelService.findChannel(market).ifPresent {
            bisqEasyOfferbookChannelSelectionService.selectChannel(it)
        }
    }

    // Private
    private fun observeSelectedChannel() {
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { marketChannel ->
                marketChannel as BisqEasyOfferbookChannel
                val market = marketChannel.market

                marketPriceService.setSelectedMarket(market)

                val title = marketChannel.shortDescription
                val iconId = "channels-" + marketChannel.id.replace(".", "-")
                val marketCodes = market.marketCodes
                val formattedPrice = marketPriceService.findMarketPrice(market)
                    .map { PriceFormatter.format(it.priceQuote, true) }
                    .orElse("")

                _selectedOfferbookMarket.value =
                    OfferbookMarket(title, iconId, marketCodes, formattedPrice)

                log.i { "selectedChannel " + marketChannel }
                log.i { "title " + title }
                log.i { "iconId " + iconId }
                log.i { "_marketCodes " + marketCodes }
                log.i { "_formattedPrice " + formattedPrice }
            }
    }
}