package network.bisq.mobile.domain.service.accounts

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

interface AccountsServiceFacade : LifeCycleAware {
    val accounts: StateFlow<List<UserDefinedFiatAccountVO>>
    val selectedAccount: StateFlow<UserDefinedFiatAccountVO?>

    suspend fun getAccounts(): List<UserDefinedFiatAccountVO>
    suspend fun addAccount(account: UserDefinedFiatAccountVO)
    suspend fun saveAccount(account: UserDefinedFiatAccountVO)
    suspend fun removeAccount(account: UserDefinedFiatAccountVO, updateSelectedAccount: Boolean = true)
    suspend fun getSelectedAccount()
    suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO)
}
