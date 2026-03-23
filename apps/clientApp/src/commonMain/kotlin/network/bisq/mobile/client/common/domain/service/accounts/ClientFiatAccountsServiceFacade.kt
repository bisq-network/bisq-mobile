package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.data.mapping.account.fiat.toDomain
import network.bisq.mobile.data.mapping.account.fiat.toDto
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.model.account.fiat.FiatAccount

class ClientFiatAccountsServiceFacade(
    private val apiGateway: FiatPaymentAccountsApiGateway,
) : FiatAccountsServiceFacade() {
    override suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRail>?): Result<List<FiatAccount>> =
        runCatching {
            apiGateway.getPaymentAccounts(paymentRails).getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeGetSelectedAccount(): Result<FiatAccount?> =
        runCatching {
            apiGateway.getSelectedAccount().getOrThrow()?.toDomain()
        }

    override suspend fun executeAddAccount(account: FiatAccount): Result<Unit> =
        runCatching {
            apiGateway.addAccount(account.toDto()).getOrThrow()
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: FiatAccount,
    ): Result<Unit> =
        runCatching {
            apiGateway.saveAccount(accountName, account.toDto()).getOrThrow()
        }

    override suspend fun executeDeleteAccount(account: FiatAccount): Result<Unit> =
        runCatching {
            apiGateway.deleteAccount(account.accountName).getOrThrow()
        }

    override suspend fun executeSetSelectedAccount(account: FiatAccount): Result<Unit> =
        runCatching {
            apiGateway.setSelectedAccount(account.toDto()).getOrThrow()
        }
}
