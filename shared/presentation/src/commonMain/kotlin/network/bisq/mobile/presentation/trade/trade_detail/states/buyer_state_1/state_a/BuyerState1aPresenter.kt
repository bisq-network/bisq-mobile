package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.common.validation.RequestedAmountValidation
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.PayoutAddressPrepRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BitcoinAddressValidation
import network.bisq.mobile.presentation.common.ui.utils.BitcoinLightningNormalization
import network.bisq.mobile.presentation.common.ui.utils.LightningInvoiceValidation
import network.bisq.mobile.presentation.main.MainPresenter

class BuyerState1aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val payoutAddressPrepRepository: PayoutAddressPrepRepository,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(BuyerState1aUiState())
    val uiState: StateFlow<BuyerState1aUiState> = _uiState.asStateFlow()

    // Guard for guardedSuspendAction, rendered as the Send button's enabled state.
    private val _isSendBitcoinPaymentDataEnabled = MutableStateFlow(true)
    val isSendBitcoinPaymentDataEnabled: StateFlow<Boolean> = _isSendBitcoinPaymentDataEnabled.asStateFlow()

    // Pre-fill from the take-offer wizard's optional address step. The value is
    // never auto-sent: while it stands unedited, Send additionally requires the explicit
    // confirmation checkbox — a distinct action closing the gap between "typed this days ago"
    // and "checked it now". Editing the field at all lifts the gate (editing IS re-engagement),
    // which also keeps the untouched path exactly as friction-free as before this feature.
    private var seededAddress: String? = null

    // The trade amount the data may request, from the same trade as the amount shown in the warning
    private var tradeAmountInSatoshi: Long? = null

    override fun onViewAttached() {
        super.onViewAttached()
        require(tradesServiceFacade.selectedTrade.value != null)
        val openTradeItemModel = tradesServiceFacade.selectedTrade.value!!
        val paymentMethod =
            openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
        tradeAmountInSatoshi = openTradeItemModel.baseAmount
        _uiState.update {
            it.copy(
                headline = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.headline.$paymentMethod".i18n(),
                description = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.description.$paymentMethod".i18n(),
                tradeAmountWithCode = openTradeItemModel.baseAmountWithCode,
                addressFieldType =
                    if (openTradeItemModel.bitcoinSettlementMethod == "LN") {
                        BitcoinLnAddressFieldType.Lightning
                    } else {
                        BitcoinLnAddressFieldType.Bitcoin
                    },
            )
        }
        seedPrefillIfAvailable(openTradeItemModel.tradeId)
    }

    fun onAction(action: BuyerState1aUiAction) {
        when (action) {
            is BuyerState1aUiAction.OnPaymentDataInput -> onPaymentDataInput(action.value, action.isValid)
            is BuyerState1aUiAction.OnConfirmPrefillChange -> _uiState.update { it.copy(hasConfirmedPrefill = action.checked) }
            BuyerState1aUiAction.OnSend -> onSend()
            BuyerState1aUiAction.OnSendAnyway -> sendPaymentData()
            BuyerState1aUiAction.OnDismissInvalidAddressDialog -> _uiState.update { it.copy(showInvalidAddressDialog = false) }
            BuyerState1aUiAction.OnDismissOtherAmountWarning -> _uiState.update { it.copy(showOtherAmountWarning = false) }
            BuyerState1aUiAction.OnOpenWalletGuide -> navigateTo(NavRoute.WalletGuideIntro)
            BuyerState1aUiAction.OnBarcodeClick -> _uiState.update { it.copy(showBarcodeView = true) }
            BuyerState1aUiAction.OnBarcodeDismiss -> _uiState.update { it.copy(showBarcodeView = false) }
            BuyerState1aUiAction.OnBarcodeFail -> _uiState.update { it.copy(showBarcodeView = false, showBarcodeError = true) }
            BuyerState1aUiAction.OnBarcodeErrorClose -> _uiState.update { it.copy(showBarcodeError = false) }
            is BuyerState1aUiAction.OnBarcodeResult -> onBarcodeResult(action.value)
        }
    }

    /**
     * Seeds the field with the address collected at take-offer time, if any. Mainchain only —
     * the wizard never collects Lightning invoices — and never over text the user already typed.
     * The wizard validated the address once already; validity is recomputed to guard a corrupted store.
     */
    private fun seedPrefillIfAvailable(tradeId: String) {
        if (_uiState.value.addressFieldType != BitcoinLnAddressFieldType.Bitcoin) return
        presenterScope.launch {
            val stored = payoutAddressPrepRepository.fetch().prefillByTradeId[tradeId]
            if (stored.isNullOrBlank() || _uiState.value.paymentData.isNotEmpty()) return@launch
            seededAddress = stored
            _uiState.update {
                it.copy(
                    paymentData = stored,
                    isPaymentDataValid = isPaymentDataValid(stored),
                    wasPrefilled = true,
                    hasConfirmedPrefill = false,
                    validationTrigger = it.validationTrigger + 1,
                )
            }
        }
    }

    private fun onPaymentDataInput(
        value: String,
        isValid: Boolean,
    ) {
        val paymentData = value.trim()
        _uiState.update {
            it.copy(
                paymentData = paymentData,
                isPaymentDataValid = isValid,
                // Any edit away from the seeded value lifts the confirmation gate permanently —
                // editing is itself the re-engagement the checkbox exists to force.
                wasPrefilled = it.wasPrefilled && paymentData == seededAddress,
            )
        }
    }

    private fun onSend() {
        val state = _uiState.value
        // UI disables Send while the gate holds; this guards the tap/recomposition race.
        if (state.wasPrefilled && !state.hasConfirmedPrefill) return
        when {
            // Over the protocol's hard cap the peer's message handler rejects unconditionally
            // (BisqEasyBtcAddressMessageHandler), so there is no "proceed anyway" for this case.
            exceedsProtocolMaxLength() -> showPaymentDataTooLongError()
            // Before the amount check, as on bisq2 desktop: the amount read from invalid data, e.g. a truncated
            // invoice, is not reliable. "Proceed anyway" still runs the amount check in sendPaymentData.
            !state.isPaymentDataValid -> _uiState.update { it.copy(showInvalidAddressDialog = true) }
            // The seller's node rejects it whatever the buyer confirms, so there is no "proceed anyway" either.
            requestsOtherAmount() -> showOtherAmountWarning()
            else -> sendPaymentData()
        }
    }

    private fun onBarcodeResult(value: String) {
        // Normalize + clean: remove scheme (case-insensitive), drop leading slashes, strip query/fragment
        val cleaned = BitcoinLightningNormalization.cleanForValidation(value)
        onPaymentDataInput(cleaned, isPaymentDataValid(cleaned))
        _uiState.update { it.copy(showBarcodeView = false, validationTrigger = it.validationTrigger + 1) }
    }

    /**
     * Validity for data the presenter sets itself (scan, prefill). The field's validation trigger only
     * refreshes its error text and never reports back, so the presenter cannot rely on it.
     */
    private fun isPaymentDataValid(value: String): Boolean =
        when (_uiState.value.addressFieldType) {
            BitcoinLnAddressFieldType.Bitcoin -> BitcoinAddressValidation.validateAddress(value)
            BitcoinLnAddressFieldType.Lightning -> LightningInvoiceValidation.validateInvoice(value)
        }

    private fun sendPaymentData() {
        val paymentData = _uiState.value.paymentData
        if (paymentData.isEmpty()) return
        // Covers the invalid-address dialog's "proceed anyway" path too.
        if (exceedsProtocolMaxLength()) {
            showPaymentDataTooLongError()
            return
        }
        if (requestsOtherAmount()) {
            showOtherAmountWarning()
            return
        }

        guardedSuspendAction(
            _isSendBitcoinPaymentDataEnabled,
            "sendBitcoinPaymentData",
            reEnableGuardOnComplete = false,
        ) {
            _uiState.update { it.copy(showInvalidAddressDialog = false) }
            tradesServiceFacade
                .buyerSendBitcoinPaymentData(paymentData)
                .onSuccess {
                    // The handoff is consumed; a lingering entry would re-seed a future
                    // screen visit for an address the peer already has.
                    tradesServiceFacade.selectedTrade.value?.let { trade ->
                        runCatching { payoutAddressPrepRepository.clearPrefill(trade.tradeId) }
                            .onFailure {
                                if (it is CancellationException) throw it
                                log.w("Failed to clear payout-address prefill", it)
                            }
                    }
                }.onFailure { exception ->
                    // Our check mirrors bisq2's; if they ever differ, the node's rejection still gets the same warning.
                    if (isOtherAmountRejection(exception)) showOtherAmountWarning() else handleError(exception)
                    _isSendBitcoinPaymentDataEnabled.value = true
                }
        }
    }

    /** Whether the data requests another amount than the trade amount, which the seller's node rejects. */
    private fun requestsOtherAmount(): Boolean {
        val tradeAmount = tradeAmountInSatoshi ?: return false
        return !RequestedAmountValidation.requestsOnlyAmount(_uiState.value.paymentData, tradeAmount)
    }

    private fun showOtherAmountWarning() {
        _uiState.update { it.copy(showOtherAmountWarning = true, showInvalidAddressDialog = false) }
    }

    // The node app throws bisq2's IllegalArgumentException; Connect gets the same text in the API's 400 body.
    private fun isOtherAmountRejection(exception: Throwable): Boolean = exception.message?.contains(OTHER_AMOUNT_REJECTION, ignoreCase = true) == true

    private fun protocolMaxLength(): Int =
        when (_uiState.value.addressFieldType) {
            BitcoinLnAddressFieldType.Bitcoin -> BitcoinAddressValidation.MAX_LENGTH
            BitcoinLnAddressFieldType.Lightning -> LightningInvoiceValidation.MAX_LENGTH
        }

    private fun exceedsProtocolMaxLength(): Boolean = _uiState.value.paymentData.length > protocolMaxLength()

    private fun showPaymentDataTooLongError() {
        showSnackbar(
            "mobile.validations.bitcoinPayment.dataTooLong".i18n(protocolMaxLength()),
            type = SnackbarType.ERROR,
        )
    }

    private companion object {
        // bisq2's RequestedAmountValidation.checkRequestsOnlyAmount message
        const val OTHER_AMOUNT_REJECTION = "must not request another amount than the trade amount"
    }
}
