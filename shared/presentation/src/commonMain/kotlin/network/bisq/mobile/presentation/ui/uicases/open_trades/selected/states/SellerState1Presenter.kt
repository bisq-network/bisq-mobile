package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class SellerState1Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val accountsServiceFacade: AccountsServiceFacade,
) : BasePresenter(mainPresenter) {

    val accounts: StateFlow<List<UserDefinedFiatAccountVO>> = accountsServiceFacade.accounts

    private var _paymentAccountData = MutableStateFlow("")
    val paymentAccountData: StateFlow<String> get() = _paymentAccountData

    private var _paymentAccountDataValid = MutableStateFlow(false)
    val paymentAccountDataValid: StateFlow<Boolean> get() = _paymentAccountDataValid

    private var _paymentAccountName = MutableStateFlow("")
    val paymentAccountName: StateFlow<String> get() = _paymentAccountName

    override fun onViewAttached() {
        super.onViewAttached()

        launchUI {
            val _accounts = withContext(IODispatcher) {
                accountsServiceFacade.getAccounts()
            }

            if (_accounts.size > 0) {
                onPaymentDataInput(_accounts[0].accountPayload.accountData, true)
                _paymentAccountName.value = _accounts[0].accountName
            }
        }
    }

    override fun onViewUnattaching() {
        _paymentAccountData.value = ""
        super.onViewUnattaching()
    }

    fun onPaymentDataInput(value: String, isValid: Boolean) {
        _paymentAccountData.value = value.trim()
        _paymentAccountDataValid.value = isValid
    }

    fun setPaymentAccountName(value: String) {
        _paymentAccountName.value = value
    }

    fun onSendPaymentData() {
        require(paymentAccountData.value.isNotEmpty())
        launchIO {
            tradesServiceFacade.sellerSendsPaymentAccount(paymentAccountData.value)
        }
    }
}