package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.node.common.domain.mapping.UserDefinedFiatAccountMapping
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeAccountsServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    AccountsServiceFacade {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = _accounts.asStateFlow()

    private val _selectedAccount = MutableStateFlow<UserDefinedFiatAccountVO?>(null)
    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = _selectedAccount.asStateFlow()

    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    private fun findBisq2Account(account: UserDefinedFiatAccountVO): UserDefinedFiatAccount? = accountService.accountByNameMap[account.accountName] as? UserDefinedFiatAccount

    override suspend fun getAccounts(): List<UserDefinedFiatAccountVO> =
        accountService
            .accountByNameMap
            .values
            .mapNotNull { (it as? UserDefinedFiatAccount)?.let(UserDefinedFiatAccountMapping::fromBisq2Model) }
            .sortedBy { it.accountName }
            .also { _accounts.value = it }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO) {
        val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(account)
        accountService.addPaymentAccount(bisq2Account)
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO) {
        selectedAccount.value?.let { existing ->
            findBisq2Account(existing)?.let { accountService.removePaymentAccount(it) }
        }
        accountService.addPaymentAccount(UserDefinedFiatAccountMapping.toBisq2Model(account))
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun removeAccount(
        account: UserDefinedFiatAccountVO,
        updateSelectedAccount: Boolean,
    ) {
        findBisq2Account(account)?.let { accountService.removePaymentAccount(it) }
        getAccounts()
        if (updateSelectedAccount) {
            val nextAccount = accounts.value.firstOrNull()
            if (nextAccount != null) {
                setSelectedAccount(nextAccount)
            } else {
                accountService.setSelectedAccount(null)
                _selectedAccount.value = null
            }
        }
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO) {
        val bisq2Account =
            findBisq2Account(account) ?: UserDefinedFiatAccountMapping.toBisq2Model(account).also {
                accountService.addPaymentAccount(it)
            }
        accountService.setSelectedAccount(bisq2Account)
        _selectedAccount.value = account
    }

    override suspend fun getSelectedAccount() {
        val optional = accountService.selectedAccount
        if (optional.isPresent) {
            val bisq2Account = optional.get() as? UserDefinedFiatAccount
            if (bisq2Account == null) {
                // Clear local state when selected account is not the expected type
                _selectedAccount.value = null
                return
            }
            _selectedAccount.value = UserDefinedFiatAccountMapping.fromBisq2Model(bisq2Account)
        } else {
            _selectedAccount.value = null
        }
    }
}
