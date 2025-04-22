package network.bisq.mobile.client.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade

class ClientAccountsServiceFacade(
    private val apiGateway: AccountsApiGateway,
) : ServiceFacade(), AccountsServiceFacade {

    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = _accounts

    private val _selectedAccount = MutableStateFlow<UserDefinedFiatAccountVO?>(null)
    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = _selectedAccount

    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getAccounts(): List<UserDefinedFiatAccountVO> {
        val result = apiGateway.getPaymentAccounts()
        if (result.isSuccess) {
            result.getOrThrow().let {
                _accounts.value = it.sortedBy { it.accountName }
            }
        }
        return _accounts.value
    }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO) {
        apiGateway.addAccount(account.accountName, account.accountPayload.accountData)
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO) {
        removeAccount(selectedAccount.value!!, false)
        apiGateway.addAccount(account.accountName, account.accountPayload.accountData)
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun removeAccount(account: UserDefinedFiatAccountVO, updateSelectedAccount: Boolean) {
        apiGateway.deleteAccount(account.accountName)
        getAccounts()
        if (updateSelectedAccount) {
            val nextAccount = accounts.value.firstOrNull()
            if (nextAccount != null) {
                setSelectedAccount(nextAccount)
            }
        }
    }

    override suspend fun getSelectedAccount() {
        val result = apiGateway.getSelectedAccount()
        if (result.isSuccess) {
            result.getOrThrow().let {
                _selectedAccount.value = it
            }
        }
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO) {
        apiGateway.setSelectedAccount(account)
        _selectedAccount.value = account
    }

}