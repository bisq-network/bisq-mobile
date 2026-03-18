package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailEnum
import network.bisq.mobile.data.replicated.api.dto.account.fiat.FiatAccountDto
import network.bisq.mobile.data.replicated.api.dto.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.node.common.domain.mapping.UserDefinedFiatAccountMapping
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeFiatAccountsServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : FiatAccountsServiceFacade() {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    override suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRailEnum>?): Result<List<FiatAccountDto>> {
        // Note: paymentRails filtering not yet implemented in node
        // Currently returns only UserDefinedFiatAccount (CUSTOM rail)
        return runCatching {
            accountService
                .accountByNameMap
                .values
                .filterIsInstance<UserDefinedFiatAccount>()
                .map { UserDefinedFiatAccountMapping.fromBisq2Model(it) }
        }
    }

    override suspend fun executeGetSelectedAccount(): Result<FiatAccountDto?> =
        runCatching {
            val optionalAccount = accountService.findSelectedAccount()
            if (optionalAccount.isPresent) {
                val bisq2Account = optionalAccount.get()
                if (bisq2Account !is UserDefinedFiatAccount) {
                    throw IllegalStateException("Selected account is not a UserDefinedFiatAccount but ${bisq2Account::class.simpleName}")
                }
                UserDefinedFiatAccountMapping.fromBisq2Model(bisq2Account)
            } else {
                null
            }
        }

    override suspend fun executeAddAccount(account: FiatAccountDto): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? UserDefinedFiatAccountDto
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount)
            accountService.addPaymentAccount(bisq2Account)
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: FiatAccountDto,
    ): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? UserDefinedFiatAccountDto
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            // updatePaymentAccount was removed in Bisq2 2.1.9; use remove + add
            val existingAccount = accountService.accountByNameMap[accountName]
            if (existingAccount != null) {
                accountService.removePaymentAccount(existingAccount)
            }
            accountService.addPaymentAccount(
                UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount),
            )
        }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> =
        runCatching {
            val account = currentState.accounts.find { it.accountName == accountName }
            if (account == null) {
                throw IllegalStateException("Account not found: $accountName")
            }
            val userDefinedAccount =
                account as? UserDefinedFiatAccountDto
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount)
            accountService.removePaymentAccount(bisq2Account)
        }

    override suspend fun executeSetSelectedAccount(account: FiatAccountDto): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? UserDefinedFiatAccountDto
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            accountService.setSelectedAccount(
                UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount),
            )
        }
}
