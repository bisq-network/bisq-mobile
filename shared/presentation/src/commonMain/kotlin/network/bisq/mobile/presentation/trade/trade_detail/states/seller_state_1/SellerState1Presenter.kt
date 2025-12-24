package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class SellerState1Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val accountsServiceFacade: AccountsServiceFacade,
) : BasePresenter(mainPresenter) {
    val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = accountsServiceFacade.accounts

    private var _paymentAccountData = MutableStateFlow("")
    val paymentAccountData: StateFlow<String> get() = _paymentAccountData.asStateFlow()

    private var _paymentAccountDataValid = MutableStateFlow(false)
    val paymentAccountDataValid: StateFlow<Boolean> get() = _paymentAccountDataValid.asStateFlow()

    private var _paymentAccountName = MutableStateFlow("")
    val paymentAccountName: StateFlow<String> get() = _paymentAccountName.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            val accounts = accountsServiceFacade.getAccounts()

            if (accounts.isNotEmpty()) {
                onPaymentDataInput(accounts[0].accountPayload.accountData, true)
                _paymentAccountName.value = accounts[0].accountName
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
