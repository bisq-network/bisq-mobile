package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.model.account.fiat.FiatAccountDto
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.utils.encodeURIParam
import network.bisq.mobile.domain.utils.Logging

class FiatPaymentAccountsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "payment-accounts/fiat"

    suspend fun getPaymentAccounts(paymentRails: Set<FiatPaymentRail>? = null): Result<List<FiatAccountDto>> {
        val queryParam =
            if (paymentRails != null && paymentRails.isNotEmpty()) {
                "?paymentRails=${paymentRails.joinToString(",") { it.name }}"
            } else {
                ""
            }
        return webSocketApiClient.get<List<FiatAccountDto>>("$basePath$queryParam")
    }

    suspend fun addAccount(account: FiatAccountDto): Result<FiatAccountDto> {
        val addFiatAccountRequest = AddFiatAccountRequest(account = account)
        return webSocketApiClient.post(basePath, addFiatAccountRequest)
    }

    suspend fun deleteAccount(accountName: String): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.delete("$basePath?accountName=$parsedAccountName")
    }

    suspend fun setSelectedAccount(account: FiatAccountDto): Result<Unit> = webSocketApiClient.patch("$basePath/selected", SetSelectedFiatAccountRequest(account))

    suspend fun getSelectedAccount(): Result<FiatAccountDto?> = webSocketApiClient.getNullable("$basePath/selected")

    suspend fun saveAccount(
        accountName: String,
        account: FiatAccountDto,
    ): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.put(
            "$basePath?accountName=$parsedAccountName",
            SaveFiatAccountRequest(account = account),
        )
    }
}
