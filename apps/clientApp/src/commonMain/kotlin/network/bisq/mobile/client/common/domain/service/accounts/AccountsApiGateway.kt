package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.encodeURIParam
import network.bisq.mobile.domain.utils.Logging

class AccountsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "payment-accounts"

    suspend fun getPaymentAccounts(): Result<List<UserDefinedFiatAccountVO>> {
        return webSocketApiClient.get<List<UserDefinedFiatAccountVO>>(basePath)
    }

    suspend fun addAccount(
        accountName: String,
        accountData: String,
    ): Result<AddAccountResponse> {
        val addAccountRequest = AddAccountRequest(
            accountName = accountName,
            accountData = accountData,
        )
        return webSocketApiClient.post(basePath, addAccountRequest)
    }

    suspend fun deleteAccount(accountName: String): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.delete("$basePath/$parsedAccountName")
    }

    suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return webSocketApiClient.patch("$basePath/selected", PaymentAccountChangeRequest(account))
    }

    suspend fun getSelectedAccount(): Result<UserDefinedFiatAccountVO> {
        return webSocketApiClient.get("$basePath/selected")
    }


}
