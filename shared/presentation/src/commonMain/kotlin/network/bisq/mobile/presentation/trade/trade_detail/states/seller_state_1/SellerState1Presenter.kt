package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.main.MainPresenter

class SellerState1Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val fiatAccountsServiceFacade: FiatAccountsServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    val accounts = _accounts.asStateFlow()

    private val _paymentAccountDataEntry =
        MutableStateFlow(
            DataEntry(validator = ::validatePaymentAccountData),
        )
    val paymentAccountDataEntry: StateFlow<DataEntry> = _paymentAccountDataEntry.asStateFlow()

    private var _paymentAccountName = MutableStateFlow("")
    val paymentAccountName: StateFlow<String> get() = _paymentAccountName.asStateFlow()

    private val minPaymentAccountDataLength = 3
    private val maxPaymentAccountDataLength = 1024

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            fiatAccountsServiceFacade
                .getAccounts()
                .onSuccess { accounts ->
                    val userDefinedAccounts = accounts.filterIsInstance<UserDefinedFiatAccountVO>()
                    _accounts.value = userDefinedAccounts
                    userDefinedAccounts.firstOrNull()?.let { account ->
                        _paymentAccountDataEntry.value =
                            _paymentAccountDataEntry.value.updateValue(
                                account.accountPayload.accountData,
                            )
                        _paymentAccountName.value = account.accountName
                    }
                }.onFailure { error ->
                    log.e(error) { "Failed to load accounts" }
                }
        }
    }

    override fun onViewUnattaching() {
        _paymentAccountDataEntry.value = _paymentAccountDataEntry.value.updateValue("")
        super.onViewUnattaching()
    }

    fun onPaymentDataInput(value: String) {
        _paymentAccountDataEntry.value = _paymentAccountDataEntry.value.updateValue(value)
    }

    private fun validatePaymentAccountData(value: String): String? =
        when {
            value.length < minPaymentAccountDataLength ->
                "mobile.bisqEasy.tradeState.info.seller.phase1.accountData.validations.minLength".i18n()

            value.length > maxPaymentAccountDataLength ->
                "mobile.bisqEasy.tradeState.info.seller.phase1.accountData.validations.maxLength".i18n()

            else -> null
        }

    fun setPaymentAccountName(value: String) {
        _paymentAccountName.value = value
    }

    fun onSendPaymentData() {
        val paymentAccountData = _paymentAccountDataEntry.value.value
        if (paymentAccountData.isEmpty()) return

        val validatedEntry = _paymentAccountDataEntry.value.validate()
        if (!validatedEntry.isValid) {
            _paymentAccountDataEntry.value = validatedEntry
            return
        }

        presenterScope.launch {
            showLoading()
            tradesServiceFacade.sellerSendsPaymentAccount(paymentAccountData)
            hideLoading()
        }
    }
}
