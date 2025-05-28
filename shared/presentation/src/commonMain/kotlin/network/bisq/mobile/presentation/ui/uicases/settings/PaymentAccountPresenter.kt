package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class PaymentAccountPresenter(
    private val accountsServiceFacade: AccountsServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IPaymentAccountSettingsPresenter {

    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> = accountsServiceFacade.accounts

    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> = accountsServiceFacade.selectedAccount


    override fun selectAccount(account: UserDefinedFiatAccountVO) {
        disableInteractive()
        presenterScope.launch {
            try {
                withContext(IODispatcher) {
                    accountsServiceFacade.setSelectedAccount(account)
                }
            } finally {
                enableInteractive()
            }
        }
    }

    override fun addAccount(newName: String, newDescription: String) {
        disableInteractive()

        if (accounts.value.find { it.accountName == newName } != null) {
            showSnackbar("Account name exists") // TODO:i18n
            enableInteractive()
            return
        }

        ioScope.launch {
            try {
                val newAccount = UserDefinedFiatAccountVO(
                    accountName = newName,
                    UserDefinedFiatAccountPayloadVO(
                        accountData = newDescription
                    )
                )
                accountsServiceFacade.addAccount(newAccount)
                showSnackbar("Account created") // TODO:i18n
            } finally {
                enableInteractive()
            }
        }
    }

    override fun saveAccount(newName: String, newDescription: String) {
        disableInteractive()
        if (selectedAccount.value?.accountName != newName && accounts.value.find { it.accountName == newName } != null) {
            showSnackbar("Account name exists") // TODO:i18n
            enableInteractive()
            return
        }

        if (selectedAccount.value != null) {
            ioScope.launch {
                try {
                    val newAccount = UserDefinedFiatAccountVO(
                        accountName = newName,
                        UserDefinedFiatAccountPayloadVO(
                            accountData = newDescription
                        )
                    )
                    accountsServiceFacade.saveAccount(newAccount)
                    showSnackbar("Account updated") // TODO:i18n
                } finally {
                    enableInteractive()
                }
            }
        } else {
            enableInteractive()
        }
    }

    override fun deleteCurrentAccount() {
        disableInteractive()
        if (selectedAccount.value != null) {
            ioScope.launch {
                runCatching {
                    accountsServiceFacade.removeAccount(selectedAccount.value!!)
                    showSnackbar("Account deleted") // TODO:i18n
                }.onFailure {
                    log.e(it) { "Couldn't remove account ${selectedAccount.value?.accountName}" }
                    showSnackbar("Unable to delete account: ${selectedAccount.value?.accountName} - Please try again")
                }
            }
        }
        enableInteractive()
    }

    override fun onViewAttached() {
        super.onViewAttached()
        ioScope.launch {
            accountsServiceFacade.getAccounts()
            accountsServiceFacade.getSelectedAccount()
        }
    }
}