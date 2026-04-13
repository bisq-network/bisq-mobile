package network.bisq.mobile.client.common.domain.service.accounts.all

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
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

    suspend fun getPaymentAccounts(): Result<List<PaymentAccountDto>> =
        webSocketApiClient.get<JsonArray>(basePath).mapCatching { jsonArray ->
            val decoded = jsonArray.mapNotNull { element -> decodePaymentAccountOrNull(element) }
            if (jsonArray.isNotEmpty() && decoded.isEmpty()) {
                throw IllegalStateException("Unable to decode any payment accounts from non-empty response")
            }
            decoded
        }

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
            account,
        )
    }

    suspend fun getFiatPaymentMethods(): Result<List<FiatPaymentMethodDto>> = webSocketApiClient.get("$paymentMethodsBasePath/fiat")

    suspend fun getCryptoPaymentMethods(): Result<List<CryptoPaymentMethodDto>> = webSocketApiClient.get("$paymentMethodsBasePath/crypto")

    private fun decodePaymentAccountOrNull(element: JsonElement): PaymentAccountDto? =
        runCatching {
            webSocketApiClient.json.decodeFromJsonElement<PaymentAccountDto>(element)
        }.getOrElse { exception ->
            log.w { "Skipping invalid payment account entry during list decode: ${exception.message}" }
            null
        }
}
