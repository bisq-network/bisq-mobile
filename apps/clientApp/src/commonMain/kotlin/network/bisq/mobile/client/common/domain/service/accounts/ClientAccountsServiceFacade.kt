package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade

class ClientAccountsServiceFacade(
    private val apiGateway: AccountsApiGateway,
) : ServiceFacade(),
    AccountsServiceFacade {

    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    override val accounts = _accounts.asStateFlow()

    private val _selectedAccount = MutableStateFlow<UserDefinedFiatAccountVO?>(null)
    override val selectedAccount = _selectedAccount.asStateFlow()

    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun getAccounts(): Result<List<UserDefinedFiatAccountVO>> {
        return runCatching {
            val result = apiGateway.getPaymentAccounts()
            result.onSuccess { accounts ->
                _accounts.value = accounts.sortedBy { it.accountName }
            }
            result.getOrThrow()
        }
    }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val addAccountResult =
                apiGateway.addAccount(account.accountName, account.accountPayload.accountData)
            addAccountResult.getOrThrow()
            getAccounts().getOrThrow()
            setSelectedAccount(account).getOrThrow()
        }
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val accountName = _selectedAccount.value?.accountName
            if (accountName == null) throw IllegalStateException("No account selected")
            apiGateway.deleteAccount(accountName).getOrThrow()
            addAccount(account).getOrThrow()
        }
    }

    override suspend fun deleteAccount(
        account: UserDefinedFiatAccountVO,
    ): Result<Unit> {
        return runCatching {
            apiGateway.deleteAccount(account.accountName).getOrThrow()
            getAccounts().getOrThrow()
            val nextAccount = accounts.value.firstOrNull()
            if (nextAccount != null) {
                setSelectedAccount(nextAccount).getOrThrow()
            } else {
                _selectedAccount.value = null
            }
        }
    }

    override suspend fun getSelectedAccount(): Result<Unit> {
        return runCatching {
            apiGateway.getSelectedAccount()
                .onSuccess { account ->
                    _selectedAccount.value = account
                }
                .getOrThrow()
            Unit
        }
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            apiGateway.setSelectedAccount(account).getOrThrow()
            _selectedAccount.value = account
        }
    }

}