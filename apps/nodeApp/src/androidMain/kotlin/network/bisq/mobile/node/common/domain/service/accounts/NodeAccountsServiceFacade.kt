package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.UserDefinedFiatAccount
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.domain.mapping.UserDefinedFiatAccountMapping
import kotlin.IllegalStateException

class NodeAccountsServiceFacade(applicationService: AndroidApplicationService.Provider) :
    AccountsServiceFacade() {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    override suspend fun executeGetAccounts(): Result<List<UserDefinedFiatAccountVO>> {
        return runCatching {
            accountService
                .accountByNameMap
                .values
                .filterIsInstance<UserDefinedFiatAccount>()
                .map { UserDefinedFiatAccountMapping.fromBisq2Model(it) }
        }
    }

    override suspend fun executeGetSelectedAccount(): Result<UserDefinedFiatAccountVO?> {
        return runCatching {
            if (accountService.selectedAccount.isPresent) {
                val bisq2Account = accountService.selectedAccount.get()
                if (bisq2Account !is UserDefinedFiatAccount) {
                    throw IllegalStateException("Selected account is not a UserDefinedFiatAccount but ${bisq2Account::class.simpleName}")
                }
                UserDefinedFiatAccountMapping.fromBisq2Model(bisq2Account)
            } else {
                null
            }
        }
    }

    override suspend fun executeAddAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(account)
            accountService.addPaymentAccount(bisq2Account)
        }
    }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: UserDefinedFiatAccountVO
    ): Result<Unit> {
        return runCatching {
            accountService.updatePaymentAccount(
                accountName, UserDefinedFiatAccountMapping.toBisq2Model(account)
            )
        }
    }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> {
        return runCatching {
            val bisq2Account = currentState.accounts
                .find { it.accountName == accountName }
                ?.let { UserDefinedFiatAccountMapping.toBisq2Model(it) }
            if (bisq2Account == null) {
                throw IllegalStateException("Account not found: $accountName")
            }
            accountService.removePaymentAccount(bisq2Account)
        }
    }

    override suspend fun executeSetSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            accountService.setSelectedAccount(
                UserDefinedFiatAccountMapping.toBisq2Model(account)
            )
        }
    }
}