package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.node.common.domain.mapping.toBisq2
import network.bisq.mobile.node.common.domain.mapping.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount as DomainCreateUserDefinedFiatAccount
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

    override suspend fun executeAddAccount(account: DomainCreateUserDefinedFiatAccount): Result<DomainUserDefinedFiatAccount> =
        runCatching {
            val bisq2Account = account.toBisq2()
            check(accountService.addPaymentAccount(bisq2Account)) {
                "Account already exists: ${bisq2Account.accountName}"
            }
            bisq2Account.toDomain()
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: DomainCreateUserDefinedFiatAccount,
    ): Result<DomainUserDefinedFiatAccount> =
        runCatching {
            val savedAccount = account.toBisq2()
            accountService.updatePaymentAccount(accountName, savedAccount)
            savedAccount.toDomain()
        }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> =
        runCatching {
            val existingAccount =
                accountService.accountByNameMap[accountName]
                    ?: throw IllegalStateException("Account not found: $accountName")

            accountService.removePaymentAccount(existingAccount)
        }

    override suspend fun executeSetSelectedAccount(accountName: String): Result<Unit> =
        runCatching {
            val account =
                accountService
                    .findAccount(accountName)
                    .orElseThrow { IllegalStateException("Account not found: $accountName") }
            check(account is UserDefinedFiatAccount) {
                "Account is not a UserDefinedFiatAccount: $accountName"
            }
            accountService.setSelectedAccount(account)
        }
}
