package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BitcoinLnAddressFieldType

class SellerState3aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private var _paymentProof: MutableStateFlow<String?> = MutableStateFlow(null)
    val paymentProof: StateFlow<String?> get() = _paymentProof

    private var _paymentProofValid: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val paymentProofValid: StateFlow<Boolean> get() = _paymentProofValid

    private val _showInvalidAddressDialog = MutableStateFlow(false)
    val showInvalidAddressDialog: StateFlow<Boolean> get() = _showInvalidAddressDialog
    fun setShowInvalidAddressDialog(value: Boolean) {
        _showInvalidAddressDialog.value = value
    }

    private var _buttonEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val buttonEnabled: StateFlow<Boolean> get() = _buttonEnabled

    private var _bitcoinAddressFieldType = MutableStateFlow(BitcoinLnAddressFieldType.Bitcoin)
    val bitcoinLnAddressFieldType: StateFlow<BitcoinLnAddressFieldType> get() = _bitcoinAddressFieldType

    private var _isLightning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLightning: StateFlow<Boolean> get() = _isLightning

    private var job: Job? = null

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        val paymentMethod = openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
        _isLightning.value = paymentMethod == "LN"
        _bitcoinAddressFieldType.value = if (paymentMethod == "LN")
            BitcoinLnAddressFieldType.Lightning
        else
            BitcoinLnAddressFieldType.Bitcoin
        updateButtonEnableState()
    }

    override fun onViewUnattaching() {
        job?.cancel()
        job = null
        _paymentProof.value = null
        super.onViewUnattaching()
    }

    fun onPaymentProofInput(value: String, isValid: Boolean) {
        _paymentProof.value = value.trim().ifEmpty { null }
        _paymentProofValid.value = isValid
        updateButtonEnableState()
    }

    fun onConfirmedBtcSent() {
        if (!paymentProofValid.value) {
            _showInvalidAddressDialog.value = true
        } else {
            confirmSend()
        }
    }

    fun confirmSend() {
        try {
            // For non-Lightning transactions, ensure payment proof is provided
            if (!isLightning.value && paymentProof.value == null) {
                _showInvalidAddressDialog.value = true
                return
            }

            job = presenterScope.launch {
                withContext(IODispatcher) {
                    tradesServiceFacade.sellerConfirmBtcSent(paymentProof.value)
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to confirm BTC sent" }
        } finally {
            setShowInvalidAddressDialog(false)
        }
    }

    private fun updateButtonEnableState() {
        _buttonEnabled.value = isLightning.value || paymentProof.value != null
    }
}