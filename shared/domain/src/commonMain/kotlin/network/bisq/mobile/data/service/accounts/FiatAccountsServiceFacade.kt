package network.bisq.mobile.data.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount

abstract class FiatAccountsServiceFacade : ServiceFacade() {
    private val _accountState = MutableStateFlow(AccountsState())
    val accountState: StateFlow<AccountsState> = _accountState.asStateFlow()
    protected val currentState: AccountsState
        get() = _accountState.value

    // Abstract methods for backend-specific operations
    protected abstract suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRail>? = null): Result<List<PaymentAccount>>

    protected abstract suspend fun executeGetSelectedAccount(): Result<PaymentAccount?>

    protected abstract suspend fun executeAddAccount(account: PaymentAccount): Result<Unit>

    protected abstract suspend fun executeSaveAccount(
        accountName: String,
        account: PaymentAccount,
    ): Result<Unit>

    protected abstract suspend fun executeDeleteAccount(account: PaymentAccount): Result<Unit>

    protected abstract suspend fun executeSetSelectedAccount(account: PaymentAccount): Result<Unit>

    // Concrete implementations with shared business logic
    suspend fun getAccounts(paymentRails: Set<FiatPaymentRail>? = null): Result<List<PaymentAccount>> =
        runCatching {
            val accounts =
                executeGetAccounts(paymentRails)
                    .getOrThrow()
                    .filterIsInstance<UserDefinedFiatAccount>()
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
                        if (account != null) {
                            state.accounts.indexOf(account)
                        } else {
                            -1
                        },
                )
            }
        }

    suspend fun addAccount(account: PaymentAccount): Result<Unit> =
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

    suspend fun saveAccount(account: PaymentAccount): Result<Unit> =
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

    suspend fun deleteAccount(account: PaymentAccount): Result<Unit> =
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
            getCurrentSelectedAccount()?.let { selectedAccount ->
                executeSetSelectedAccount(selectedAccount).getOrThrow()
            }
        }

    // Protected helper methods
    protected fun getSortedAccounts(accounts: List<PaymentAccount>) = accounts.sortedBy { it.accountName }

    protected fun getCurrentSelectedAccount() = currentState.accounts.getOrNull(currentState.selectedAccountIndex)

    protected fun getAccountsExcluding(accountName: String): List<PaymentAccount> = currentState.accounts.filter { it.accountName != accountName }
}
