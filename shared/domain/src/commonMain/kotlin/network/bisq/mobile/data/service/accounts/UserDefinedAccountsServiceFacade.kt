package network.bisq.mobile.data.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount

abstract class UserDefinedAccountsServiceFacade : ServiceFacade() {
    private val _accountState = MutableStateFlow(AccountsState())
    val accountState: StateFlow<AccountsState> = _accountState.asStateFlow()
    protected val currentState: AccountsState
        get() = _accountState.value

    // Abstract methods for backend-specific operations
    protected abstract suspend fun executeGetAccounts(): Result<List<UserDefinedFiatAccount>>

    protected abstract suspend fun executeGetSelectedAccount(): Result<UserDefinedFiatAccount?>

    protected abstract suspend fun executeAddAccount(account: UserDefinedFiatAccount): Result<Unit>

    protected abstract suspend fun executeSaveAccount(
        accountName: String,
        account: UserDefinedFiatAccount,
    ): Result<Unit>

    protected abstract suspend fun executeDeleteAccount(account: UserDefinedFiatAccount): Result<Unit>

    protected abstract suspend fun executeSetSelectedAccount(account: UserDefinedFiatAccount): Result<Unit>

    // Concrete implementations with shared business logic
    suspend fun getAccounts(): Result<List<UserDefinedFiatAccount>> =
        runCatching {
            val accounts =
                executeGetAccounts()
                    .getOrThrow()
            val sortedAccounts = getSortedAccounts(accounts)
            _accountState.update {
                it.copy(accounts = sortedAccounts)
            }
            sortedAccounts
        }

    suspend fun getSelectedAccount(): Result<Unit> =
        runCatching {
            val account = executeGetSelectedAccount().getOrThrow()
            _accountState.update { state ->
                state.copy(
                    selectedAccountIndex =
                        account
                            ?.let(state.accounts::indexOf)
                            ?.takeIf { it >= 0 }
                            ?: 0,
                )
            }
        }

    suspend fun addAccount(account: UserDefinedFiatAccount): Result<Unit> =
        runCatching {
            executeAddAccount(account).getOrThrow()
            val accounts = _accountState.value.accounts
            val sortedAccounts = getSortedAccounts(accounts + account)
            val selectedIndex = sortedAccounts.indexOf(account)
            _accountState.update {
                it.copy(
                    accounts = sortedAccounts,
                    selectedAccountIndex = selectedIndex,
                )
            }
            setSelectedAccountIndex(selectedIndex)
        }

    suspend fun saveAccount(account: UserDefinedFiatAccount): Result<Unit> =
        runCatching {
            val accountName = getCurrentSelectedAccount()?.accountName
            if (accountName == null) throw IllegalStateException("No account selected")
            executeSaveAccount(accountName, account).getOrThrow()
            val accountList = getAccountsExcluding(accountName)
            val sortedAccounts = getSortedAccounts(accountList + account)
            val selectedIndex = sortedAccounts.indexOf(account)
            _accountState.update {
                it.copy(
                    accounts = sortedAccounts,
                    selectedAccountIndex = selectedIndex,
                )
            }
        }

    suspend fun deleteAccount(account: UserDefinedFiatAccount): Result<Unit> =
        runCatching {
            val selectedAccount = getCurrentSelectedAccount()
            executeDeleteAccount(account).getOrThrow()
            val accountList = getAccountsExcluding(account.accountName)
            val newSelectedIndex =
                if (selectedAccount?.accountName == account.accountName && accountList.isNotEmpty()) {
                    0
                } else {
                    accountList.indexOf(selectedAccount)
                }
            _accountState.update { currentState ->
                currentState.copy(
                    accounts = accountList,
                    selectedAccountIndex = newSelectedIndex,
                )
            }
            setSelectedAccountIndex(newSelectedIndex)
        }

    suspend fun setSelectedAccountIndex(accountIndex: Int): Result<Unit> =
        runCatching {
            val currentSelectedIndex = _accountState.value.selectedAccountIndex
            if (currentSelectedIndex != accountIndex) {
                _accountState.update { it.copy(selectedAccountIndex = accountIndex) }
            }
            val selectedAccount = _accountState.value.accounts.getOrNull(accountIndex)
            selectedAccount?.let {
                executeSetSelectedAccount(it).getOrThrow()
            }
        }

    // Protected helper methods
    protected fun getSortedAccounts(accounts: List<UserDefinedFiatAccount>) = accounts.sortedBy { it.accountName }

    protected fun getCurrentSelectedAccount() =
        currentState.accounts.getOrNull(currentState.selectedAccountIndex)
            ?: currentState.accounts.firstOrNull()

    protected fun getAccountsExcluding(accountName: String): List<UserDefinedFiatAccount> = currentState.accounts.filter { it.accountName != accountName }
}
