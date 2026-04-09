package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BitcoinLightningNormalization
import network.bisq.mobile.presentation.main.MainPresenter

class BuyerState1aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
) : BasePresenter(mainPresenter) {
    private var _headline = MutableStateFlow("")
    val headline: StateFlow<String> = _headline.asStateFlow()

    private var _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private var _bitcoinPaymentData = MutableStateFlow("")
    val bitcoinPaymentData: StateFlow<String> = _bitcoinPaymentData.asStateFlow()

    private var _bitcoinPaymentDataValid = MutableStateFlow(false)
    val bitcoinPaymentDataValid: StateFlow<Boolean> = _bitcoinPaymentDataValid.asStateFlow()

    private var _bitcoinAddressFieldType = MutableStateFlow(BitcoinLnAddressFieldType.Bitcoin)
    val bitcoinLnAddressFieldType: StateFlow<BitcoinLnAddressFieldType> = _bitcoinAddressFieldType.asStateFlow()

    private val _showInvalidAddressDialog = MutableStateFlow(false)
    val showInvalidAddressDialog: StateFlow<Boolean> = _showInvalidAddressDialog.asStateFlow()

    private val _showBarcodeView = MutableStateFlow(false)
    val showBarcodeView: StateFlow<Boolean> = _showBarcodeView.asStateFlow()

    private val _showBarcodeError = MutableStateFlow(false)
    val showBarcodeError: StateFlow<Boolean> = _showBarcodeError.asStateFlow()

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
        _bitcoinAddressFieldType.value =
            if (openTradeItemModel.bitcoinSettlementMethod == "LN") {
                BitcoinLnAddressFieldType.Lightning
            } else {
                BitcoinLnAddressFieldType.Bitcoin
            }
    }

    fun onBitcoinPaymentDataInput(
        value: String,
        isValid: Boolean,
    ) {
        _bitcoinPaymentData.value = value.trim()
        _bitcoinPaymentDataValid.value = isValid
    }

    fun onSendBitcoinPaymentDataClick() {
        if (!bitcoinPaymentDataValid.value) {
            setShowInvalidAddressDialog(true)
        } else {
            sendBitcoinPaymentData()
        }
    }

    fun onBarcodeClick() {
        _showBarcodeView.value = true
    }

    fun onBarcodeFail() {
        _showBarcodeView.value = false
        _showBarcodeError.value = true
    }

    fun onBarcodeErrorClose() {
        _showBarcodeError.value = false
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

    fun sendBitcoinPaymentData() {
        val bitcoinPaymentData = bitcoinPaymentData.value
        if (bitcoinPaymentData.isEmpty()) return
        setShowInvalidAddressDialog(false)
        presenterScope.launch {
            showLoading()
            tradesServiceFacade
                .buyerSendBitcoinPaymentData(bitcoinPaymentData)
                .onFailure { exception ->
                    handleError(exception)
                }
            hideLoading()
        }
    }

    fun onOpenWalletGuide() {
        navigateTo(NavRoute.WalletGuideIntro)
    }
}
