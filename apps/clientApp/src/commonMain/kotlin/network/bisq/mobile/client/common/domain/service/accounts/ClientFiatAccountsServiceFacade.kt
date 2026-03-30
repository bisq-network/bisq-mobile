package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.data.mapping.account.toDomain
import network.bisq.mobile.data.mapping.account.toDto
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount

class ClientFiatAccountsServiceFacade(
    private val apiGateway: FiatPaymentAccountsApiGateway,
) : FiatAccountsServiceFacade() {
    override suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRail>?): Result<List<PaymentAccount>> =
        runCatching {
            apiGateway.getPaymentAccounts(paymentRails).getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeGetSelectedAccount(): Result<PaymentAccount?> =
        runCatching {
            apiGateway.getSelectedAccount().getOrThrow()?.toDomain()
        }

    override suspend fun executeAddAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            apiGateway.addAccount(account.toDto()).getOrThrow()
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: PaymentAccount,
    ): Result<Unit> =
        runCatching {
            apiGateway.saveAccount(accountName, account.toDto()).getOrThrow()
        }

    override suspend fun executeDeleteAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            apiGateway.deleteAccount(account.accountName).getOrThrow()
        }

    override suspend fun executeSetSelectedAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            apiGateway.setSelectedAccount(account.toDto()).getOrThrow()
        }
}
