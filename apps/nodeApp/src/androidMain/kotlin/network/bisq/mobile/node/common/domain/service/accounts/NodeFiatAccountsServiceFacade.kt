package network.bisq.mobile.node.common.domain.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.data.replicated.account.fiat.FiatAccountVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailEnum
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.node.common.domain.mapping.UserDefinedFiatAccountMapping
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import kotlin.IllegalStateException

class NodeFiatAccountsServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : FiatAccountsServiceFacade() {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    override suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRailEnum>?): Result<List<FiatAccountVO>> {
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

    override suspend fun executeGetSelectedAccount(): Result<FiatAccountVO?> =
        runCatching {
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

    override suspend fun executeAddAccount(account: FiatAccountVO): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? UserDefinedFiatAccountVO
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount)
            accountService.addPaymentAccount(bisq2Account)
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: FiatAccountVO,
    ): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? UserDefinedFiatAccountVO
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            accountService.updatePaymentAccount(
                accountName,
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
                account as? UserDefinedFiatAccountVO
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount)
            accountService.removePaymentAccount(bisq2Account)
        }

    override suspend fun executeSetSelectedAccount(account: FiatAccountVO): Result<Unit> =
        runCatching {
            val userDefinedAccount =
                account as? UserDefinedFiatAccountVO
                    ?: throw IllegalStateException("Account is not a UserDefinedFiatAccountVO but ${account::class.simpleName}")
            accountService.setSelectedAccount(
                UserDefinedFiatAccountMapping.toBisq2Model(userDefinedAccount),
            )
        }
}
