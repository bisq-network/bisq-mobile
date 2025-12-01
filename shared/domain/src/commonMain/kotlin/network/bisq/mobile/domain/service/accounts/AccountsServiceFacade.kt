package network.bisq.mobile.domain.service.accounts

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

interface AccountsServiceFacade : LifeCycleAware {
    val accounts: StateFlow<List<UserDefinedFiatAccountVO>>
    val selectedAccount: StateFlow<UserDefinedFiatAccountVO?>

    suspend fun getAccounts(): Result<List<UserDefinedFiatAccountVO>>
    suspend fun addAccount(account: UserDefinedFiatAccountVO): Result<Unit>
    suspend fun saveAccount(account: UserDefinedFiatAccountVO): Result<Unit>
    suspend fun deleteAccount(account: UserDefinedFiatAccountVO): Result<Unit>
    suspend fun getSelectedAccount(): Result<Unit>
    suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit>
}
