package network.bisq.mobile.domain.service.accounts

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.account.AccountVO

interface AccountsServiceFacade : LifeCycleAware {
    val accounts: StateFlow<List<AccountVO<*, *>>>
    val selectedAccount: StateFlow<AccountVO<*, *>?>

    suspend fun getAccounts(): List<AccountVO<*, *>>
    suspend fun addAccount(account: AccountVO<*, *>)
    suspend fun removeAccount(account: AccountVO<*, *>)
    suspend fun setSelectedAccount(account: AccountVO<*, *>?)
}
