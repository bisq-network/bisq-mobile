package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.utils.encodeURIParam
import network.bisq.mobile.domain.utils.Logging

class FiatPaymentAccountsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "payment-accounts/fiat"

    suspend fun getPaymentAccounts(): Result<List<PaymentAccountDto>> = webSocketApiClient.get(basePath)

    suspend fun addAccount(account: PaymentAccountDto): Result<PaymentAccountDto> {
        val addFiatAccountRequest = AddFiatAccountRequest(account = account)
        return webSocketApiClient.post(basePath, addFiatAccountRequest)
    }

    suspend fun deleteAccount(accountName: String): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.delete("$basePath?accountName=$parsedAccountName")
    }

    suspend fun setSelectedAccount(account: PaymentAccountDto): Result<Unit> = webSocketApiClient.patch("$basePath/selected", SetSelectedFiatAccountRequest(account))

    suspend fun getSelectedAccount(): Result<PaymentAccountDto?> = webSocketApiClient.getNullable("$basePath/selected")

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
}
