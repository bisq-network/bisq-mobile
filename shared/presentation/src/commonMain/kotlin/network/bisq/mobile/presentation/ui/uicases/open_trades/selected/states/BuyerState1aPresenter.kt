package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.ui.helpers.BitcoinLightningNormalization
import network.bisq.mobile.presentation.ui.navigation.NavRoute

class BuyerState1aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {

    private var _headline = MutableStateFlow("")
    val headline: StateFlow<String> get() = _headline.asStateFlow()

    private var _description = MutableStateFlow("")
    val description: StateFlow<String> get() = _description.asStateFlow()

    private var _bitcoinPaymentData = MutableStateFlow("")
    val bitcoinPaymentData: StateFlow<String> get() = _bitcoinPaymentData.asStateFlow()

    private var _bitcoinPaymentDataValid = MutableStateFlow(false)
    val bitcoinPaymentDataValid: StateFlow<Boolean> get() = _bitcoinPaymentDataValid.asStateFlow()

    private var _bitcoinAddressFieldType = MutableStateFlow(BitcoinLnAddressFieldType.Bitcoin)
    val bitcoinLnAddressFieldType: StateFlow<BitcoinLnAddressFieldType> get() = _bitcoinAddressFieldType.asStateFlow()

    private val _showInvalidAddressDialog = MutableStateFlow(false)
    val showInvalidAddressDialog: StateFlow<Boolean> get() = _showInvalidAddressDialog.asStateFlow()

    private val _showBarcodeView = MutableStateFlow(false)
    val showBarcodeView: StateFlow<Boolean> = _showBarcodeView.asStateFlow()

    private val _triggerBitcoinLnAddressValidation = MutableStateFlow(0)
    val triggerBitcoinLnAddressValidation = _triggerBitcoinLnAddressValidation.asStateFlow()

    fun setShowInvalidAddressDialog(value: Boolean) {
        _showInvalidAddressDialog.value = value
    }

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        val paymentMethod =
            openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
        _headline.value =
            "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.headline.$paymentMethod".i18n()
        _description.value =
            "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.description.$paymentMethod".i18n()
        _bitcoinAddressFieldType.value = if (openTradeItemModel.bitcoinSettlementMethod == "LN")
            BitcoinLnAddressFieldType.Lightning
        else
            BitcoinLnAddressFieldType.Bitcoin
    }


    fun onBitcoinPaymentDataInput(value: String, isValid: Boolean) {
        _bitcoinPaymentData.value = value.trim()
        _bitcoinPaymentDataValid.value = isValid
    }

    fun onSendClick() {
        if (!bitcoinPaymentDataValid.value) {
            setShowInvalidAddressDialog(true)
        } else {
            onSend()
        }
    }

    fun onBarcodeClick() {
        _showBarcodeView.value = true
    }

    fun onBarcodeViewDismiss() {
        _showBarcodeView.value = false
    }

    fun onBarcodeResult(value: String) {
        // Normalize + clean: remove scheme (case-insensitive), drop leading slashes, strip query/fragment
        val cleaned = BitcoinLightningNormalization.cleanForValidation(value)
        onBitcoinPaymentDataInput(cleaned, false)
        _showBarcodeView.value = false
        _triggerBitcoinLnAddressValidation.value++
    }

    fun onSend() {
        require(bitcoinPaymentData.value.isNotEmpty())
        setShowInvalidAddressDialog(false)

        launchUI {
            withContext(IODispatcher) {
                tradesServiceFacade.buyerSendBitcoinPaymentData(bitcoinPaymentData.value)
            }
        }
    }

    fun onOpenWalletGuide() {
        navigateTo(NavRoute.WalletGuideIntro)
    }
}