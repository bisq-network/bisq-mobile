package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.crypto.CryptoPaymentMethodDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodDto
import network.bisq.mobile.data.utils.encodeURIParam
import network.bisq.mobile.domain.utils.Logging

class PaymentAccountsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "payment-accounts"
    private val paymentMethodsBasePath = "payment-accounts/payment-methods"

    suspend fun getPaymentAccounts(): Result<List<PaymentAccountDto>> = webSocketApiClient.get<List<PaymentAccountDto>>("$basePath")

    suspend fun addAccount(account: PaymentAccountDto): Result<PaymentAccountDto> = webSocketApiClient.post(basePath, account)

    suspend fun deleteAccount(accountName: String): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.delete("$basePath?accountName=$parsedAccountName")
    }

    suspend fun saveAccount(
        accountName: String,
        account: PaymentAccountDto,
    ): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.put(
            "$basePath?accountName=$parsedAccountName",
            SaveFiatAccountRequest(account = account),
        )
    }

    suspend fun getFiatPaymentMethods(): Result<List<FiatPaymentMethodDto>> = webSocketApiClient.get("$paymentMethodsBasePath/fiat")

    suspend fun getCryptoPaymentMethods(): Result<List<CryptoPaymentMethodDto>> = webSocketApiClient.get("$paymentMethodsBasePath/crypto")
}
