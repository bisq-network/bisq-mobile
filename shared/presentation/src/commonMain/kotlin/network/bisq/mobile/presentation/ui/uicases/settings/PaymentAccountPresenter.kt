package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.i18n.i18n
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
        launchUI {
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
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            enableInteractive()
            return
        }

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
                enableInteractive()
            }
        }
    }

    override fun saveAccount(newName: String, newDescription: String) {
        disableInteractive()
        if (selectedAccount.value?.accountName != newName && accounts.value.find { it.accountName == newName } != null) {
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            enableInteractive()
            return
        }

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
            launchIO {
                try {
                    accountsServiceFacade.removeAccount(selectedAccount.value!!)
                    showSnackbar("mobile.user.paymentAccounts.createAccount.notifications.name.accountDeleted".i18n())
                } catch (e: Exception) {
                    log.e { "Couldn't remove account ${selectedAccount.value?.accountName}" }
                    showSnackbar("mobile.user.paymentAccounts.createAccount.notifications.name.unableToDelete".i18n(selectedAccount.value?.accountName ?: ""))

                } finally {
                    enableInteractive()
                }
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        launchIO {
            accountsServiceFacade.getAccounts()
            accountsServiceFacade.getSelectedAccount()
        }
    }
}