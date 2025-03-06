package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.AccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.AccountVO
import network.bisq.mobile.domain.data.replicated.payment.PaymentMethodVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PaymentAccount

open class PaymentAccountPresenter(
    private val settingsRepository: SettingsRepository,
    private val accountsServiceFacade: AccountsServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IPaymentAccountSettingsPresenter {

    override val accounts: StateFlow<List<AccountVO<*, *>>> = accountsServiceFacade.accounts

    override val selectedAccount: StateFlow<AccountVO<*, *>?> = accountsServiceFacade.selectedAccount


    override fun selectAccount(account: AccountVO<*, *>) {
        backgroundScope.launch {
            accountsServiceFacade.setSelectedAccount(account)
        }
    }

    override fun addAccount(newName: String, newDescription: String) {
        /*
        val newAccount = AccountVO(
            accountName = newName,
            accountPayload = AccountPayloadVO(
                id = "random-string", // TODO
                paymentMethodName = newName,
                accountData = newDescription
            ),
            paymentMethod = PaymentMethodVO(

            )
        )

        val updatedAccounts = _accounts.value.toMutableList().apply {
            add(newAccount)
        }
        _accounts.value = updatedAccounts
        _selectedAccount.value = newAccount
        */
    }

    override fun saveAccount(newName: String, newDescription: String) {
        /*
        val updatedAccounts = _accounts.value.map {
            if (it.id == _selectedAccount.value.id) {
                it.copy(name = newName, description = newDescription)
            } else it
        }
        _accounts.value = updatedAccounts
        _selectedAccount.value = updatedAccounts.first { it.id == _selectedAccount.value.id }
        */
    }

    override fun deleteCurrentAccount() {
        /*
        val updatedAccounts = _accounts.value.toMutableList()
        updatedAccounts.remove(_selectedAccount.value)
        _accounts.value = updatedAccounts
        _selectedAccount.value = updatedAccounts.firstOrNull() ?: PaymentAccount("0", "", "")
        */
    }

}