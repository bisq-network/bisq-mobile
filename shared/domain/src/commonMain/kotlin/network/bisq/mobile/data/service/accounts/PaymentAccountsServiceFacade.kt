package network.bisq.mobile.data.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod

abstract class PaymentAccountsServiceFacade : ServiceFacade() {
    private val _accounts = MutableStateFlow<List<PaymentAccount>>(emptyList())
    val accounts = _accounts.asStateFlow()

    // Abstract methods for backend-specific operations
    protected abstract suspend fun executeGetAccounts(): Result<List<PaymentAccount>>

    protected abstract suspend fun executeAddAccount(account: PaymentAccount): Result<PaymentAccount>

    protected abstract suspend fun executeSaveAccount(
        accountName: String,
        account: PaymentAccount,
    ): Result<Unit>

    protected abstract suspend fun executeDeleteAccount(account: PaymentAccount): Result<Unit>

    protected abstract suspend fun executeGetFiatPaymentMethods(): Result<List<FiatPaymentMethod>>

    protected abstract suspend fun executeGetCryptoPaymentMethods(): Result<List<CryptoPaymentMethod>>

    // Concrete implementations with shared business logic
    suspend fun getAccounts(): Result<Unit> =
        runCatching {
            val accounts =
                executeGetAccounts()
                    .getOrThrow()
            val sortedAccounts = getSortedAccounts(accounts)
            _accounts.update { sortedAccounts }
        }

    suspend fun addAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            val addedAccount =
                executeAddAccount(account)
                    .getOrThrow()
            _accounts.update { current ->
                getSortedAccounts(current + addedAccount)
            }
        }

    suspend fun deleteAccount(account: PaymentAccount): Result<Unit> =
        runCatching {
            executeDeleteAccount(account).getOrThrow()
            _accounts.update { current ->
                getSortedAccounts(current.filter { it.accountName != account.accountName })
            }
        }

    suspend fun getFiatPaymentMethods(): Result<List<FiatPaymentMethod>> =
        runCatching {
            executeGetFiatPaymentMethods().getOrThrow()
        }

    suspend fun getCryptoPaymentMethods(): Result<List<CryptoPaymentMethod>> =
        runCatching {
            executeGetCryptoPaymentMethods().getOrThrow()
        }

    // Protected helper methods
    protected fun getSortedAccounts(accounts: List<PaymentAccount>) = accounts.sortedBy { it.accountName }

    protected fun getAccountsExcluding(accountName: String): List<PaymentAccount> = _accounts.value.filter { it.accountName != accountName }
}
