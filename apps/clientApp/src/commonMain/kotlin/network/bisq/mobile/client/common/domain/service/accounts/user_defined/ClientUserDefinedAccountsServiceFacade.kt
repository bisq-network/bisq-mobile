package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import network.bisq.mobile.client.payment_accounts.data.mapping.fiat.toDomain
import network.bisq.mobile.client.payment_accounts.data.mapping.fiat.toDto
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount

class ClientUserDefinedAccountsServiceFacade(
    private val apiGateway: UserDefinedPaymentAccountsApiGateway,
) : UserDefinedAccountsServiceFacade() {
    override suspend fun executeGetAccounts(): Result<List<UserDefinedFiatAccount>> =
        runCatching {
            apiGateway.getPaymentAccounts().getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeGetSelectedAccount(): Result<UserDefinedFiatAccount?> =
        runCatching {
            apiGateway.getSelectedAccount().getOrThrow()?.toDomain()
        }

    override suspend fun executeAddAccount(account: CreateUserDefinedFiatAccount): Result<UserDefinedFiatAccount> =
        runCatching {
            apiGateway.addAccount(account.toDto()).getOrThrow().toDomain()
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: CreateUserDefinedFiatAccount,
    ): Result<UserDefinedFiatAccount> =
        runCatching {
            apiGateway.saveAccount(accountName, account.toDto()).getOrThrow().toDomain()
        }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> =
        runCatching {
            apiGateway.deleteAccount(accountName).getOrThrow()
        }

    override suspend fun executeSetSelectedAccount(accountName: String): Result<Unit> =
        runCatching {
            apiGateway.setSelectedAccount(accountName).getOrThrow()
        }
}
