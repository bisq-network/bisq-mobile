package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.replicated.account.fiat.FiatAccountVO
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailEnum
import network.bisq.mobile.domain.encodeURIParam
import network.bisq.mobile.domain.utils.Logging

class FiatPaymentAccountsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "payment-accounts/fiat"

    suspend fun getPaymentAccounts(paymentRails: Set<FiatPaymentRailEnum>? = null): Result<List<FiatAccountVO>> {
        val queryParam =
            if (paymentRails != null && paymentRails.isNotEmpty()) {
                "?paymentRails=${paymentRails.joinToString(",") { it.name }}"
            } else {
                ""
            }
        return webSocketApiClient.get<List<FiatAccountVO>>("$basePath$queryParam")
    }

    suspend fun addAccount(account: FiatAccountVO): Result<FiatAccountVO> {
        val addFiatAccountRequest = AddFiatAccountRequest(account = account)
        return webSocketApiClient.post(basePath, addFiatAccountRequest)
    }

    suspend fun deleteAccount(accountName: String): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.delete("$basePath?accountName=$parsedAccountName")
    }

    suspend fun setSelectedAccount(account: FiatAccountVO): Result<Unit> = webSocketApiClient.patch("$basePath/selected", SetSelectedFiatAccountRequest(account))

    suspend fun getSelectedAccount(): Result<FiatAccountVO?> = webSocketApiClient.getNullable("$basePath/selected")

    suspend fun saveAccount(
        accountName: String,
        account: FiatAccountVO,
    ): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.put(
            "$basePath?accountName=$parsedAccountName",
            SaveFiatAccountRequest(account = account),
        )
    }
}
