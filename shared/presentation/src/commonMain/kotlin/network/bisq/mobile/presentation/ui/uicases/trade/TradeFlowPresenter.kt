package network.bisq.mobile.presentation.ui.uicases.trade

import androidx.compose.runtime.Composable
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

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

open class TradeFlowPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
) : BasePresenter(mainPresenter), ITradeFlowPresenter {

    override val offerListItems: StateFlow<List<OfferListItemVO>> = offerbookServiceFacade.offerListItems

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

    private val _showCloseTradeDialog = MutableStateFlow(false)
    override val showCloseTradeDialog: StateFlow<Boolean> get() = _showCloseTradeDialog
    override fun setShowCloseTradeDialog(value: Boolean) {
        _showCloseTradeDialog.value = value
    }

    override fun closeTrade() {
        setShowCloseTradeDialog(true)
    }

    private val _showCancelTradeDialog = MutableStateFlow(false)
    override val showCancelTradeDialog: StateFlow<Boolean> get() = _showCancelTradeDialog
    override fun setShowCancelTradeDialog(value: Boolean) {
        _showCancelTradeDialog.value = value
    }

    override fun cancelTrade() {
        setShowCancelTradeDialog(true)
    }

    private val _showMediationDialog = MutableStateFlow(false)
    override val showMediationDialog: StateFlow<Boolean> get() = _showMediationDialog
    override fun setShowMediationDialog(value: Boolean) {
        _showMediationDialog.value = value
    }

    override fun closeTradeConfirm() {
        setShowCloseTradeDialog(false)
        rootNavigator.popBackStack(Routes.Offerbook.name, inclusive = false, saveState = false)
    }

    override fun openWalletGuideLink() {
        // Open web link for Wallet guide
    }

    override fun openTradeGuideLink() {
        // Open web link for Trade guide
    }

    override fun exportTrade() {
        // TODO
    }

    override fun openMediation() {
        // TODO: Is Mediation part of MVP?
        // If not, should we show a message to the user,
        // to install desktop app to handle disputes?
    }

    override fun onViewAttached() {
    }

    override fun onViewUnattaching() {
    }

    override fun goToChat() {
        rootNavigator.navigate(Routes.ChatScreen.name)
    }
}
