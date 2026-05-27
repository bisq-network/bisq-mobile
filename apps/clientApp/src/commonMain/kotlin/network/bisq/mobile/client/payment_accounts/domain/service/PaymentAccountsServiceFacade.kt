package network.bisq.mobile.client.payment_accounts.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

abstract class PaymentAccountsServiceFacade : ServiceFacade() {
    private val _accountsByName = MutableStateFlow<Map<String, PaymentAccount>>(emptyMap())
    val accountsByName = _accountsByName.asStateFlow()
    val accountsFlow = accountsByName.map { getSortedAccounts(it.values.toList()) }

    // Abstract methods for backend-specific operations
    protected abstract suspend fun executeGetAccounts(): Result<List<PaymentAccount>>

    protected abstract suspend fun executeAddAccount(account: CreatePaymentAccount): Result<PaymentAccount>

    protected abstract suspend fun executeDeleteAccount(accountName: String): Result<Unit>

    protected abstract suspend fun executeGetFiatPaymentMethods(): Result<List<FiatPaymentMethod>>

    protected abstract suspend fun executeGetCryptoPaymentMethods(): Result<List<CryptoPaymentMethod>>

    protected abstract suspend fun executeGetBankAccountCountryDetails(countryCode: String): Result<BankAccountCountryDetails>

    // Concrete implementations with shared business logic
    suspend fun getAccounts(): Result<Unit> =
        runCatching {
            val accounts =
                executeGetAccounts()
                    .getOrThrow()
            _accountsByName.update { getAccountsByName(accounts) }
        }

    suspend fun addAccount(account: CreatePaymentAccount): Result<Unit> =
        runCatching {
            val addedAccount =
                executeAddAccount(account)
                    .getOrThrow()
            _accountsByName.update { current ->
                getAccountsByName(current.values.toList() + addedAccount)
            }
        }

    suspend fun deleteAccount(accountName: String): Result<Unit> =
        runCatching {
            executeDeleteAccount(accountName).getOrThrow()
            _accountsByName.update { current ->
                getAccountsByName(current.values.filter { it.accountName != accountName })
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

    suspend fun getBankAccountCountryDetails(countryCode: String): Result<BankAccountCountryDetails> =
        runCatching {
            executeGetBankAccountCountryDetails(countryCode).getOrThrow()
        }

    // Protected helper methods
    protected fun getSortedAccounts(accounts: List<PaymentAccount>) = accounts.sortedBy { it.accountName }

    protected fun getAccountsByName(accounts: List<PaymentAccount>): Map<String, PaymentAccount> =
        buildMap {
            accounts.forEach { account ->
                val existing = put(account.accountName, account)
                require(existing == null) { "Duplicate accountName found: ${account.accountName}" }
            }
        }
}
