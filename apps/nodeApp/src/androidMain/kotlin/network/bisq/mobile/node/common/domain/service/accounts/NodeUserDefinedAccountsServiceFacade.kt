package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.node.common.domain.mapping.toBisq2
import network.bisq.mobile.node.common.domain.mapping.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount as DomainUserDefinedFiatAccount

class NodeUserDefinedAccountsServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : UserDefinedAccountsServiceFacade() {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    override suspend fun executeGetAccounts(): Result<List<DomainUserDefinedFiatAccount>> =
        runCatching {
            accountService
                .accountByNameMap
                .values
                .filterIsInstance<UserDefinedFiatAccount>()
                .map { it.toDomain() }
        }

    override suspend fun executeGetSelectedAccount(): Result<DomainUserDefinedFiatAccount?> =
        runCatching {
            val optionalAccount = accountService.findSelectedAccount()
            if (optionalAccount.isPresent) {
                val bisq2Account = optionalAccount.get()
                if (bisq2Account !is UserDefinedFiatAccount) {
                    throw IllegalStateException("Selected account is not a UserDefinedFiatAccount but ${bisq2Account::class.simpleName}")
                }
                bisq2Account.toDomain()
            } else {
                null
            }
        }

    override suspend fun executeAddAccount(account: DomainUserDefinedFiatAccount): Result<Unit> =
        runCatching {
            val bisq2Account = account.toBisq2()
            accountService.addPaymentAccount(bisq2Account)
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: DomainUserDefinedFiatAccount,
    ): Result<Unit> =
        runCatching {
            // updatePaymentAccount was removed in Bisq2 2.1.9; use remove + add
            val existingAccount = accountService.accountByNameMap[accountName]
            if (existingAccount != null) {
                accountService.removePaymentAccount(existingAccount)
            }
            accountService.addPaymentAccount(
                account.toBisq2(),
            )
        }

    override suspend fun executeDeleteAccount(account: DomainUserDefinedFiatAccount): Result<Unit> =
        runCatching {
            val existingAccount =
                accountService.accountByNameMap[account.accountName]
                    ?: throw IllegalStateException("Account not found: ${account.accountName}")

            accountService.removePaymentAccount(existingAccount)
        }

    override suspend fun executeSetSelectedAccount(account: DomainUserDefinedFiatAccount): Result<Unit> =
        runCatching {
            accountService.setSelectedAccount(
                account.toBisq2(),
            )
        }
}
