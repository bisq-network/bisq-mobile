package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import network.bisq.mobile.data.mapping.account.fiat.toDomain
import network.bisq.mobile.data.mapping.account.fiat.toDto
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
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

    override suspend fun executeAddAccount(account: UserDefinedFiatAccount): Result<Unit> =
        runCatching {
            apiGateway.addAccount(account.toDto()).getOrThrow()
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: UserDefinedFiatAccount,
    ): Result<Unit> =
        runCatching {
            apiGateway.saveAccount(accountName, account.toDto()).getOrThrow()
        }

    override suspend fun executeDeleteAccount(account: UserDefinedFiatAccount): Result<Unit> =
        runCatching {
            apiGateway.deleteAccount(account.accountName).getOrThrow()
        }

    override suspend fun executeSetSelectedAccount(account: UserDefinedFiatAccount): Result<Unit> =
        runCatching {
            apiGateway.setSelectedAccount(account.toDto()).getOrThrow()
        }
}
