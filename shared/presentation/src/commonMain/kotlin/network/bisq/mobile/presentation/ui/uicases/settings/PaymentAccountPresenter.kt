package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PaymentAccount

open class PaymentAccountPresenter(
    private val settingsRepository: SettingsRepository,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IPaymentAccountSettingsPresenter {

    private val _accounts = MutableStateFlow(listOf<PaymentAccount>())
    override val accounts: StateFlow<List<PaymentAccount>> = _accounts

    private val _selectedAccount = MutableStateFlow(_accounts.value.firstOrNull() ?: PaymentAccount(id= "0", name= "", description = ""))
    override val selectedAccount: StateFlow<PaymentAccount> = _selectedAccount

    override fun selectAccount(account: PaymentAccount) {
        _selectedAccount.value = _accounts.value.firstOrNull { it.id == account.id }
                ?: account
    }

    override fun addAccount(newName: String, newDescription: String) {
        val newAccount = PaymentAccount(
            id = _accounts.value.count().toString(),
            name = newName,
            description = newDescription
        )

        val updatedAccounts = _accounts.value.toMutableList().apply {
            add(newAccount)
        }
        _accounts.value = updatedAccounts
        _selectedAccount.value = newAccount
    }

    override fun saveAccount(newName: String, newDescription: String) {
        val updatedAccounts = _accounts.value.map {
            if (it.id == _selectedAccount.value.id) {
                it.copy(name = newName, description = newDescription)
            } else it
        }
        _accounts.value = updatedAccounts
        _selectedAccount.value = updatedAccounts.first { it.id == _selectedAccount.value.id }
    }

    override fun deleteCurrentAccount() {
        val updatedAccounts = _accounts.value.toMutableList()
        updatedAccounts.remove(_selectedAccount.value)
        _accounts.value = updatedAccounts
        _selectedAccount.value = updatedAccounts.firstOrNull() ?: PaymentAccount("0", "", "")
    }

}