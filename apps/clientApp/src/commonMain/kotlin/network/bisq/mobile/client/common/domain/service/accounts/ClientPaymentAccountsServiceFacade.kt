package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.data.mapping.account.crypto.toDomain
import network.bisq.mobile.data.mapping.account.fiat.toDomain
import network.bisq.mobile.data.mapping.account.toDomain
import network.bisq.mobile.data.mapping.account.toDto
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod

class ClientPaymentAccountsServiceFacade(
    private val apiGateway: PaymentAccountsApiGateway,
) : PaymentAccountsServiceFacade() {
    override suspend fun executeGetAccounts(): Result<List<PaymentAccount>> =
        runCatching {
            apiGateway.getPaymentAccounts().getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeAddAccount(account: PaymentAccount): Result<PaymentAccount> =
        runCatching {
            apiGateway.addAccount(account.toDto()).getOrThrow().toDomain()
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

    override suspend fun executeGetFiatPaymentMethods(): Result<List<FiatPaymentMethod>> =
        runCatching {
            apiGateway.getFiatPaymentMethods().getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeGetCryptoPaymentMethods(): Result<List<CryptoPaymentMethod>> =
        runCatching {
            apiGateway.getCryptoPaymentMethods().getOrThrow().map { it.toDomain() }
        }
}
