package network.bisq.mobile.android.node.service.accounts

import bisq.account.AccountService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.account.AccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeAccountsServiceFacade(applicationService: AndroidApplicationService.Provider) : AccountsServiceFacade, Logging {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    private val _accounts = MutableStateFlow<List<AccountVO<*, *>>>(emptyList())
    override val accounts: StateFlow<List<AccountVO<*, *>>> get() = _accounts

    private val _selectedAccount = MutableStateFlow<AccountVO<*, *>?>(null)
    override val selectedAccount: StateFlow<AccountVO<*, *>?> get() = _selectedAccount

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    override suspend fun getAccounts(): List<AccountVO<*, *>> {
        return accountService.getAccountByNameMap().values.map { it.toVO() }.also {
            _accounts.value = it
        }
    }

    override suspend fun findAccountByName(accountName: String): AccountVO<*, *>? {
        return accountService.findAccount(accountName).map { it.toVO() }.orElse(null)
    }

    override suspend fun addAccount(account: AccountVO<*, *>) {
        accountService.addPaymentAccount(account.toDomain())
        refreshAccounts()
    }

    override suspend fun removeAccount(account: AccountVO<*, *>) {
        accountService.removePaymentAccount(account.toDomain())
        refreshAccounts()
    }

    override suspend fun setSelectedAccount(account: AccountVO<*, *>?) {
        accountService.setSelectedAccount(account?.toDomain())
        _selectedAccount.value = account
    }

    override fun activate() {
        log.i("Activating NodeAccountsServiceFacade")
        refreshAccounts()
        _isInitialized.value = true
    }

    override fun deactivate() {
        log.i("Deactivating NodeAccountsServiceFacade")
        _isInitialized.value = false
    }

    private fun refreshAccounts() {
        _accounts.value = accountService.getAccountByNameMap().values.map { it.toVO() }
        _selectedAccount.value = accountService.getSelectedAccount().map { it.toVO() }.orElse(null)
    }
}