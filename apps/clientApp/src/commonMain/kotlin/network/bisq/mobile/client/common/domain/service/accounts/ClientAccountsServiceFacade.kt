package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade

class ClientAccountsServiceFacade(
    private val apiGateway: UserDefinedFiatAccountsApiGateway,
) : AccountsServiceFacade() {

    override suspend fun executeGetAccounts(): Result<List<UserDefinedFiatAccountVO>> {
        return apiGateway.getPaymentAccounts()
    }

    override suspend fun executeGetSelectedAccount(): Result<UserDefinedFiatAccountVO?> {
        return runCatching {
            apiGateway.getSelectedAccount().getOrThrow()
        }
    }

    override suspend fun executeAddAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return runCatching {
            apiGateway.addAccount(account.accountName, account.accountPayload.accountData)
                .getOrThrow()
        }
    }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: UserDefinedFiatAccountVO
    ): Result<Unit> {
        return apiGateway.saveAccount(accountName, account)
    }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> {
        return apiGateway.deleteAccount(accountName)
    }

    override suspend fun executeSetSelectedAccount(account: UserDefinedFiatAccountVO): Result<Unit> {
        return apiGateway.setSelectedAccount(account)
    }
}