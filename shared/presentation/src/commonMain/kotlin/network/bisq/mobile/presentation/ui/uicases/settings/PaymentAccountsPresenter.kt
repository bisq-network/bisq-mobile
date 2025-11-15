package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class PaymentAccountsPresenter(
    private val accountsServiceFacade: AccountsServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IPaymentAccountSettingsPresenter {

    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = accountsServiceFacade.accounts

    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = accountsServiceFacade.selectedAccount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        launchIO {
            try {
                _isLoading.value = true
                accountsServiceFacade.getAccounts()
                accountsServiceFacade.getSelectedAccount()
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun selectAccount(account: UserDefinedFiatAccountVO) {
        disableInteractive()
        launchUI {
            try {
                withContext(Dispatchers.IO) {
                    accountsServiceFacade.setSelectedAccount(account)
                }
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
        launchIO {
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
            launchIO {
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
            launchIO {
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