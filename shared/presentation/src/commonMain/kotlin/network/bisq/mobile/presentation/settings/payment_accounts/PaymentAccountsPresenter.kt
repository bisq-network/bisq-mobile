package network.bisq.mobile.presentation.settings.payment_accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class PaymentAccountsPresenter(
    private val accountsServiceFacade: AccountsServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IPaymentAccountSettingsPresenter {

    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = accountsServiceFacade.accounts

    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = accountsServiceFacade.selectedAccount

    private val _isLoadingAccounts = MutableStateFlow(false)
    val isLoadingAccounts = _isLoadingAccounts.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch {
            try {
                _isLoadingAccounts.value = true
                accountsServiceFacade.getAccounts()
                accountsServiceFacade.getSelectedAccount()
            } finally {
                _isLoadingAccounts.value = false
            }
        }
    }

    override fun selectAccount(account: UserDefinedFiatAccountVO) {
        disableInteractive()
        presenterScope.launch {
            try {
                accountsServiceFacade.setSelectedAccount(account)
            } finally {
                enableInteractive()
            }
        }
    }

    override fun addAccount(newName: String, newDescription: String) {
        if (accounts.value.find { it.accountName == newName } != null) {
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            return
        }
        showLoading()
        presenterScope.launch {
            try {
                val newAccount = UserDefinedFiatAccountVO(
                    accountName = newName,
                    UserDefinedFiatAccountPayloadVO(
                        accountData = newDescription
                    )
                )
                accountsServiceFacade.addAccount(newAccount)
                showSnackbar("mobile.user.paymentAccounts.createAccount.notifications.name.accountCreated".i18n())
            } finally {
                hideLoading()
            }
        }
    }

    override fun saveAccount(newName: String, newDescription: String) {
        if (selectedAccount.value?.accountName != newName && accounts.value.find { it.accountName == newName } != null) {
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            return
        }
        showLoading()
        if (selectedAccount.value != null) {
            presenterScope.launch {
                try {
                    val newAccount = UserDefinedFiatAccountVO(
                        accountName = newName,
                        UserDefinedFiatAccountPayloadVO(
                            accountData = newDescription
                        )
                    )
                    accountsServiceFacade.saveAccount(newAccount)
                    showSnackbar("mobile.user.paymentAccounts.createAccount.notifications.name.accountUpdated".i18n())
                } finally {
                    hideLoading()
                }
            }
        } else {
            hideLoading()
        }
    }

    override fun deleteCurrentAccount() {
        if (selectedAccount.value != null) {
            showLoading()
            presenterScope.launch {
                try {
                    accountsServiceFacade.removeAccount(selectedAccount.value!!)
                    showSnackbar("mobile.user.paymentAccounts.createAccount.notifications.name.accountDeleted".i18n())
                } catch (e: Exception) {
                    log.e { "Couldn't remove account ${selectedAccount.value?.accountName}" }
                    showSnackbar("mobile.user.paymentAccounts.createAccount.notifications.name.unableToDelete".i18n(selectedAccount.value?.accountName ?: ""))

                } finally {
                    hideLoading()
                }
            }
        }
    }

}