package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import network.bisq.mobile.data.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.node.common.domain.mapping.toBisq2
import network.bisq.mobile.node.common.domain.mapping.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount as DomainUserDefinedFiatAccount

class NodeFiatAccountsServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : FiatAccountsServiceFacade() {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    override suspend fun executeGetAccounts(): Result<List<PaymentAccount>> =
        runCatching {
            accountService
                .accountByNameMap
                .values
                .filterIsInstance<UserDefinedFiatAccount>()
                .map { it.toDomain() }
        }

    override suspend fun executeGetSelectedAccount(): Result<PaymentAccount?> =
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

    override suspend fun executeAddAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? DomainUserDefinedFiatAccount
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccount but ${account::class.simpleName}")
            val bisq2Account = userDefinedAccount.toBisq2()
            accountService.addPaymentAccount(bisq2Account)
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: PaymentAccount,
    ): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? DomainUserDefinedFiatAccount
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccount but ${account::class.simpleName}")
            // updatePaymentAccount was removed in Bisq2 2.1.9; use remove + add
            val existingAccount = accountService.accountByNameMap[accountName]
            if (existingAccount != null) {
                accountService.removePaymentAccount(existingAccount)
            }
            accountService.addPaymentAccount(
                userDefinedAccount.toBisq2(),
            )
        }

    override suspend fun executeDeleteAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? DomainUserDefinedFiatAccount
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccount but ${account::class.simpleName}")

            val existingAccount =
                accountService.accountByNameMap[userDefinedAccount.accountName]
                    ?: throw IllegalStateException("Account not found: ${userDefinedAccount.accountName}")

            accountService.removePaymentAccount(existingAccount)
        }

    override suspend fun executeSetSelectedAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? DomainUserDefinedFiatAccount
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccount but ${account::class.simpleName}")
            accountService.setSelectedAccount(
                userDefinedAccount.toBisq2(),
            )
        }
}
