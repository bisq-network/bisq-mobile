package network.bisq.mobile.domain.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.ServiceFacade

abstract class AccountsServiceFacade : ServiceFacade() {

    private val _accountState = MutableStateFlow(AccountsState())
    val accountState: StateFlow<AccountsState> = _accountState.asStateFlow()
    protected val currentState: AccountsState
        get() = _accountState.value

    // Abstract methods for backend-specific operations
    protected abstract suspend fun executeGetAccounts(): Result<List<UserDefinedFiatAccountVO>>
    protected abstract suspend fun executeGetSelectedAccount(): Result<UserDefinedFiatAccountVO?>
    protected abstract suspend fun executeAddAccount(account: UserDefinedFiatAccountVO): Result<Unit>
    protected abstract suspend fun executeSaveAccount(
        accountName: String,
        account: UserDefinedFiatAccountVO
    ): Result<Unit>

    protected abstract suspend fun executeDeleteAccount(accountName: String): Result<Unit>
    protected abstract suspend fun executeSetSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit>

    // Concrete implementations with shared business logic
    suspend fun getAccounts(): Result<List<UserDefinedFiatAccountVO>> {
        return runCatching {
            val accounts = executeGetAccounts().getOrThrow()
            _accountState.update {
                it.copy(accounts = getSortedAccounts(accounts))
            }
            accounts
        }
    }

    suspend fun getSelectedAccount(): Result<Unit> {
        return runCatching {
            val account = executeGetSelectedAccount().getOrThrow()
            _accountState.update { state ->
                state.copy(
                    selectedAccountIndex = if (account != null) {
                        state.accounts.indexOf(account)
                    } else {
                        -1
                    }
                )
            }
        }
    }

    suspend fun addAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            executeAddAccount(account).getOrThrow()
            val accounts = _accountState.value.accounts
            val sortedAccounts = getSortedAccounts(accounts + account)
            val selectedIndex = sortedAccounts.indexOf(account)
            _accountState.update {
                it.copy(
                    accounts = sortedAccounts,
                    selectedAccountIndex = selectedIndex
                )
            }
            setSelectedAccountIndex(selectedIndex)
        }
    }

    suspend fun saveAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val accountName = getCurrentSelectedAccount()?.accountName
            if (accountName == null) throw IllegalStateException("No account selected")
            executeSaveAccount(accountName, account).getOrThrow()
            val accountList = deleteAccountLocally(accountName)
            val sortedAccounts = getSortedAccounts(accountList + account)
            val selectedIndex = sortedAccounts.indexOf(account)
            _accountState.update {
                it.copy(
                    accounts = sortedAccounts,
                    selectedAccountIndex = selectedIndex
                )
            }
        }
    }

    suspend fun deleteAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val selectedAccount = getCurrentSelectedAccount()
            executeDeleteAccount(account.accountName).getOrThrow()
            val accountList = deleteAccountLocally(account.accountName)
            val newSelectedIndex =
                if (selectedAccount?.accountName == account.accountName && accountList.isNotEmpty()) {
                    0
                } else {
                    accountList.indexOf(selectedAccount)
                }
            _accountState.update { currentState ->
                currentState.copy(
                    accounts = accountList,
                    selectedAccountIndex = newSelectedIndex
                )
            }
            setSelectedAccountIndex(newSelectedIndex)
        }
    }

    suspend fun setSelectedAccountIndex(accountIndex: Int): Result<Unit> {
        return runCatching {
            val currentSelectedIndex = _accountState.value.selectedAccountIndex
            if (currentSelectedIndex != accountIndex) {
                _accountState.update { it.copy(selectedAccountIndex = accountIndex) }
            }
            getCurrentSelectedAccount()?.let { selectedAccount ->
                executeSetSelectedAccount(selectedAccount).getOrThrow()
            }
        }
    }

    // Protected helper methods
    protected fun getSortedAccounts(accounts: List<UserDefinedFiatAccountVO>) =
        accounts.sortedBy { it.accountName }

    protected fun getCurrentSelectedAccount() =
        currentState.accounts.getOrNull(currentState.selectedAccountIndex)

    protected fun deleteAccountLocally(accountName: String): List<UserDefinedFiatAccountVO> {
        return currentState.accounts.filter { it.accountName != accountName }
    }
}
