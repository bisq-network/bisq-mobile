package network.bisq.mobile.presentation.ui.uicases.trades

import androidx.compose.runtime.Composable
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

enum class TradeFlowScreenSteps(val titleKey: String) {
    ACCOUNT_DETAILS(titleKey = "bisqEasy_tradeState_phase1"),
    FIAT_PAYMENT(titleKey = "bisqEasy_tradeState_phase2"),
    BITCOIN_TRANSFER(titleKey = "bisqEasy_tradeState_phase3"),
    TRADE_COMPLETED(titleKey = "bisqEasy_tradeState_phase4")
}

@Composable
fun TradeFlowScreenSteps.getTranslatedTitle(): String {
    val strings = LocalStrings.current.bisqEasyTradeState
    return when (this) {
        TradeFlowScreenSteps.ACCOUNT_DETAILS -> strings.bisqEasy_tradeState_phase1
        TradeFlowScreenSteps.FIAT_PAYMENT -> strings.bisqEasy_tradeState_phase2
        TradeFlowScreenSteps.BITCOIN_TRANSFER -> strings.bisqEasy_tradeState_phase3
        TradeFlowScreenSteps.TRADE_COMPLETED -> strings.bisqEasy_tradeState_phase4
    }
}

// TODO: Should do Interface for this?
open class TradeFlowPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), ITradeFlowPresenter {

    override val offerListItems: StateFlow<List<OfferListItem>> = offerbookServiceFacade.offerListItems

    override val steps = listOf(
        TradeFlowScreenSteps.ACCOUNT_DETAILS,
        TradeFlowScreenSteps.FIAT_PAYMENT,
        TradeFlowScreenSteps.BITCOIN_TRANSFER,
        TradeFlowScreenSteps.TRADE_COMPLETED
    )

    // Could be onchain address or LN Invoice
    private val _receiveAddress = MutableStateFlow("")
    override val receiveAddress: StateFlow<String> get() = _receiveAddress
    override fun setReceiveAddress(value: String) {
        _receiveAddress.value = value
    }

    private val _confirmingFiatPayment = MutableStateFlow(false)
    override val confirmingFiatPayment: StateFlow<Boolean> get() = _confirmingFiatPayment
    override fun setConfirmingFiatPayment(value: Boolean) {
        _confirmingFiatPayment.value = value
    }

    override fun confirmFiatPayment() {
        setConfirmingFiatPayment(true)
    }


    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }
}
