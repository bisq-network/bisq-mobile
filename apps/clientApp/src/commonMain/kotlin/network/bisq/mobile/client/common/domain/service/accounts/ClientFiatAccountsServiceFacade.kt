package network.bisq.mobile.client.common.domain.service.accounts

import network.bisq.mobile.domain.data.replicated.account.fiat.FiatAccountVO
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailEnum
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade

class ClientFiatAccountsServiceFacade(
    private val apiGateway: FiatPaymentAccountsApiGateway,
) : FiatAccountsServiceFacade() {
    override suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRailEnum>?): Result<List<FiatAccountVO>> =
        runCatching {
            apiGateway.getPaymentAccounts(paymentRails).getOrThrow()
        }

    override suspend fun executeGetSelectedAccount(): Result<FiatAccountVO?> =
        runCatching {
            apiGateway.getSelectedAccount().getOrThrow()
        }

    override suspend fun executeAddAccount(account: FiatAccountVO): Result<Unit> =
        runCatching {
            apiGateway.addAccount(account).getOrThrow()
        }

    override suspend fun executeSaveAccount(
        accountName: String,
        account: FiatAccountVO,
    ): Result<Unit> =
        runCatching {
            apiGateway.saveAccount(accountName, account).getOrThrow()
        }

    override suspend fun executeDeleteAccount(accountName: String): Result<Unit> =
        runCatching {
            apiGateway.deleteAccount(accountName).getOrThrow()
        }

    override suspend fun executeSetSelectedAccount(account: FiatAccountVO): Result<Unit> =
        runCatching {
            apiGateway.setSelectedAccount(account).getOrThrow()
        }
}
