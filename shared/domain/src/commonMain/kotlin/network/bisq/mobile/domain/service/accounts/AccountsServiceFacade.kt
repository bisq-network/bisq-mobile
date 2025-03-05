package network.bisq.mobile.domain.service.accounts

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.account.AccountVO

interface AccountsServiceFacade : LifeCycleAware {

    suspend fun getAccounts(): Result<List<AccountVO>>

    val selectedAccount: StateFlow<AccountVO?>
    suspend fun setSelectedAccount(account: AccountVO)

    suspend fun addAccount(account: AccountVO)
    suspend fun removeAccount(account: AccountVO)
}