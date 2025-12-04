package network.bisq.mobile.android.node.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.UserDefinedFiatAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.UserDefinedFiatAccountMapping
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade

class NodeAccountsServiceFacade(applicationService: AndroidApplicationService.Provider) :
    ServiceFacade(), AccountsServiceFacade {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

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
            accountService
                .accountByNameMap
                .values
                .filterIsInstance<UserDefinedFiatAccount>()
                .map { UserDefinedFiatAccountMapping.fromBisq2Model(it) }
                .sortedBy { it.accountName }
                .also { _accounts.value = it }
        }
    }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(account)
            accountService.addPaymentAccount(bisq2Account)
            getAccounts().getOrThrow()
            setSelectedAccount(account).getOrThrow()
        }
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val selectedAccount = selectedAccount.value
            if (selectedAccount == null) throw IllegalStateException("No account selected")
            accountService.removePaymentAccount(
                UserDefinedFiatAccountMapping.toBisq2Model(selectedAccount)
            )
            addAccount(account).getOrThrow()
        }
    }

    override suspend fun deleteAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            accountService.removePaymentAccount(UserDefinedFiatAccountMapping.toBisq2Model(account))
            getAccounts().getOrThrow()
            val nextAccount = accounts.value.firstOrNull()
            if (nextAccount != null) {
                setSelectedAccount(nextAccount).getOrThrow()
            } else {
                _selectedAccount.value = null
            }
        }
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            accountService.setSelectedAccount(UserDefinedFiatAccountMapping.toBisq2Model(account))
            _selectedAccount.value = account
        }
    }

    override suspend fun getSelectedAccount(): Result<Unit> {
        return runCatching {
            if (accountService.selectedAccount.isPresent) {
                val bisq2Account = accountService.selectedAccount.get()
                if (bisq2Account !is UserDefinedFiatAccount) {
                    throw IllegalStateException("Selected account is not a UserDefinedFiatAccount but ${bisq2Account::class.simpleName}")
                }
                val account = UserDefinedFiatAccountMapping.fromBisq2Model(bisq2Account)
                _selectedAccount.value = account
            } else {
                _selectedAccount.value = null
            }
        }
    }
}