package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class PaymentAccountPresenter(
    private val settingsRepository: SettingsRepository,
    private val accountsServiceFacade: AccountsServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IPaymentAccountSettingsPresenter {

    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> = accountsServiceFacade.accounts

    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> = accountsServiceFacade.selectedAccount


    override fun selectAccount(account: UserDefinedFiatAccountVO) {
        backgroundScope.launch {
            accountsServiceFacade.setSelectedAccount(account)
        }
    }

    override fun addAccount(newName: String, newDescription: String) {

        backgroundScope.launch {
            val newAccount = UserDefinedFiatAccountVO(
                accountName = newName,
                UserDefinedFiatAccountPayloadVO(
                    accountData = newDescription
                )
            )

            accountsServiceFacade.addAccount(newAccount)
        }
    }

    override fun saveAccount(newName: String, newDescription: String) {
        if (selectedAccount.value != null) {
            backgroundScope.launch {
                val newAccount = UserDefinedFiatAccountVO(
                    accountName = newName,
                    UserDefinedFiatAccountPayloadVO(
                        accountData = newDescription
                    )
                )
                accountsServiceFacade.saveAccount(newAccount)
            }
        }
    }

    override fun deleteCurrentAccount() {
        if (selectedAccount.value != null) {
            backgroundScope.launch {
                accountsServiceFacade.removeAccount(selectedAccount.value!!)
                showSnackbar("Account deleted")
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        backgroundScope.launch {
            accountsServiceFacade.getAccounts()
            accountsServiceFacade.getSelectedAccount()
        }
    }
}