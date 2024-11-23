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


class SelectedMarket(private val applicationServiceSupplier: AndroidApplicationService.Supplier) {


    private val log = Logger.withTag(this::class.simpleName ?: "MarketChannel")

    private lateinit var bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService
    private lateinit var bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService
    private lateinit var marketPriceService: MarketPriceService

    private lateinit var selectedChannelPin: Pin


    private val _title = MutableStateFlow("")
    val title: StateFlow<String> get() = _title

    private val _iconId = MutableStateFlow("")
    val iconId: StateFlow<String> get() = _iconId

    private val _marketCodes = MutableStateFlow("")
    val marketCodes: StateFlow<String> get() = _marketCodes

    private val _formattedPrice = MutableStateFlow("")
    val formattedPrice: StateFlow<String> get() = _formattedPrice


    fun initialize() {
        bisqEasyOfferbookChannelService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelService
        bisqEasyOfferbookChannelSelectionService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelSelectionService
        marketPriceService =
            applicationServiceSupplier.bondedRolesServiceSupplier.get().marketPriceService
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
                channel as BisqEasyOfferbookChannel
                val market = channel.market
                marketPriceService.setSelectedMarket(market)

                _title.value = channel.shortDescription
                _iconId.value = "channels-" + channel.id.replace(".", "-")
                _marketCodes.value = market.marketCodes

                _formattedPrice.value = marketPriceService.findMarketPrice(market)
                    .map { PriceFormatter.format(it.priceQuote, true) }
                    .orElse("")

                log.i { "selectedChannel " + channel }
                log.i { "title " + title.value }
                log.i { "iconId " + iconId.value }
                log.i { "_marketCodes " + _marketCodes.value }
                log.i { "_formattedPrice " + _formattedPrice.value }
            }
    }

    fun resume() {
    }

    fun dispose() {
    }

    fun selectMarket(market: Market) {
        log.i { "selectMarket " + market }
        bisqEasyOfferbookChannelService.findChannel(market).ifPresent {
            bisqEasyOfferbookChannelSelectionService.selectChannel(it)
        }
    }
}