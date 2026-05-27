package network.bisq.mobile.client.payment_accounts.data.service

import io.ktor.http.HttpStatusCode
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.payment_accounts.data.mapping.crypto.toDomain
import network.bisq.mobile.client.payment_accounts.data.mapping.fiat.toDomain
import network.bisq.mobile.client.payment_accounts.data.mapping.toDomain
import network.bisq.mobile.client.payment_accounts.data.mapping.toDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.repository.BankAccountCountryDetailsRepository
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountNameAlreadyExistsException
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

class ClientPaymentAccountsServiceFacade(
    private val apiGateway: PaymentAccountsApiGateway,
    private val bankAccountCountryDetailsRepository: BankAccountCountryDetailsRepository,
) : PaymentAccountsServiceFacade() {
    override suspend fun executeGetAccounts(): Result<List<PaymentAccount>> =
        runCatching {
            apiGateway.getPaymentAccounts().getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeAddAccount(account: CreatePaymentAccount): Result<PaymentAccount> =
        runCatching {
            apiGateway.addAccount(account.toDto()).getOrThrow().toDomain()
        }.recoverCatching { exception ->
            if (exception is WebSocketRestApiException && exception.httpStatusCode == HttpStatusCode.Conflict) {
                throw PaymentAccountNameAlreadyExistsException(exception.message)
            }
            throw exception
        }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> =
        runCatching {
            apiGateway.deleteAccount(accountName).getOrThrow()
        }

    override suspend fun executeGetFiatPaymentMethods(): Result<List<FiatPaymentMethod>> =
        runCatching {
            apiGateway.getFiatPaymentMethods().getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeGetCryptoPaymentMethods(): Result<List<CryptoPaymentMethod>> =
        runCatching {
            apiGateway.getCryptoPaymentMethods().getOrThrow().map { it.toDomain() }
        }

    override suspend fun executeGetBankAccountCountryDetails(countryCode: String): Result<BankAccountCountryDetails> =
        runCatching {
            bankAccountCountryDetailsRepository
                .get(countryCode) {
                    apiGateway.getBankAccountCountryDetails().getOrThrow()
                }.toDomain()
        }
}
