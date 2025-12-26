package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class SellerState1Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val fiatAccountsServiceFacade: FiatAccountsServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    val accounts = _accounts.asStateFlow()

    private val _paymentAccountData = MutableStateFlow("")
    val paymentAccountData: StateFlow<String> get() = _paymentAccountData.asStateFlow()

    private val _paymentAccountDataValid = MutableStateFlow(false)
    val paymentAccountDataValid: StateFlow<Boolean> get() = _paymentAccountDataValid.asStateFlow()

    private var _paymentAccountName = MutableStateFlow("")
    val paymentAccountName: StateFlow<String> get() = _paymentAccountName.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            fiatAccountsServiceFacade
                .getAccounts()
                .onSuccess { accounts ->
                    val userDefinedAccounts = accounts.filterIsInstance<UserDefinedFiatAccountVO>()
                    _accounts.value = userDefinedAccounts
                    userDefinedAccounts.firstOrNull()?.let { account ->
                        onPaymentDataInput(account.accountPayload.accountData, true)
                        _paymentAccountName.value = account.accountName
                    }
                }.onFailure { error ->
                    log.e(error) { "Failed to load accounts" }
                }
        }
    }

    override fun onViewUnattaching() {
        _paymentAccountData.value = ""
        super.onViewUnattaching()
    }

    fun onPaymentDataInput(
        value: String,
        isValid: Boolean,
    ) {
        _paymentAccountData.value = value.trim()
        _paymentAccountDataValid.value = isValid
    }

    fun setPaymentAccountName(value: String) {
        _paymentAccountName.value = value
    }

    fun onSendPaymentData() {
        val paymentAccountData = paymentAccountData.value
        if (paymentAccountData.isEmpty()) return
        presenterScope.launch {
            showLoading()
            tradesServiceFacade.sellerSendsPaymentAccount(paymentAccountData)
            hideLoading()
        }
    }
}
