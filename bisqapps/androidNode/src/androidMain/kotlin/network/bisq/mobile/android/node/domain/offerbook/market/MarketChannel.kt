package network.bisq.mobile.android.node.domain.offerbook.market

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService
import bisq.common.observable.Pin
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import java.util.Locale


class MarketChannel(private val applicationServiceSupplier: AndroidApplicationService.Supplier) {

    private val log = Logger.withTag(this::class.simpleName ?: "NodeOfferbookServiceFacade")

    private lateinit var bisqEasyOfferbookChannelSelectionService: BisqEasyOfferbookSelectionService
    private lateinit var selectedChannelPin: Pin


    private val _title = MutableStateFlow("")
    val title: StateFlow<String> get() = _title

    private val _iconId = MutableStateFlow("")
    val iconId: StateFlow<String> get() = _iconId

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> get() = _description

    private val _fiatAmountTitle = MutableStateFlow("")
    val fiatAmountTitle: StateFlow<String> get() = _fiatAmountTitle


    fun initialize() {
        //applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelService
        bisqEasyOfferbookChannelSelectionService =
            applicationServiceSupplier.chatServiceSupplier.get().bisqEasyOfferbookChannelSelectionService
        selectedChannelPin =
            bisqEasyOfferbookChannelSelectionService.selectedChannel.addObserver { channel ->
                channel as BisqEasyOfferbookChannel
                _title.value = channel.shortDescription
                _iconId.value = "channels-" + channel.id.replace(".", "-")
                _description.value = channel.getDisplayString()
                _fiatAmountTitle.value =
                    channel.market.quoteCurrencyCode.uppercase(Locale.getDefault()) + " amount" // todo use i18n

                log.i { "channel " + channel}
                log.i { "title " + title.value }
                log.i { "iconId " + iconId.value }
                log.i { "description " + description.value }
                log.i { "fiatAmountTitle " + fiatAmountTitle.value }
            }
    }

    fun resume() {
    }

    fun dispose() {
    }
}