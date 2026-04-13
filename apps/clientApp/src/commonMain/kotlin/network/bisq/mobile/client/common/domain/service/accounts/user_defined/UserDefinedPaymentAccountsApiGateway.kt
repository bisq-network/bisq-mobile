package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.utils.encodeURIParam
import network.bisq.mobile.domain.utils.Logging

class UserDefinedPaymentAccountsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "payment-accounts/fiat"

    suspend fun getPaymentAccounts(): Result<List<UserDefinedFiatAccountDto>> = webSocketApiClient.get(basePath)

    suspend fun addAccount(account: UserDefinedFiatAccountDto): Result<UserDefinedFiatAccountDto> {
        val addUserDefinedAccountRequest = AddUserDefinedAccountRequest(account = account)
        return webSocketApiClient.post(basePath, addUserDefinedAccountRequest)
    }

    suspend fun deleteAccount(accountName: String): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.delete("$basePath?accountName=$parsedAccountName")
    }

    suspend fun setSelectedAccount(account: UserDefinedFiatAccountDto): Result<Unit> =
        webSocketApiClient.patch(
            "$basePath/selected",
            SetSelectedUserDefinedAccountRequest(account),
        )

    suspend fun getSelectedAccount(): Result<UserDefinedFiatAccountDto?> = webSocketApiClient.getNullable("$basePath/selected")

    suspend fun saveAccount(
        accountName: String,
        account: UserDefinedFiatAccountDto,
    ): Result<Unit> {
        val parsedAccountName = encodeURIParam(accountName)
        return webSocketApiClient.put(
            "$basePath?accountName=$parsedAccountName",
            SaveUserDefinedAccountRequest(account = account),
        )
    }
}
