package network.bisq.mobile.client.common.domain.service.accounts.all

import io.ktor.http.HttpStatusCode
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.data.mapping.account.crypto.toDomain
import network.bisq.mobile.data.mapping.account.fiat.toDomain
import network.bisq.mobile.data.mapping.account.toDomain
import network.bisq.mobile.data.mapping.account.toDto
import network.bisq.mobile.data.service.accounts.PaymentAccountNameAlreadyExistsException
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.repository.BankAccountCountryDetailsRepository

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
