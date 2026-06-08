package network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateSameBankAccountPayload(
    val selectedCountryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String? = null,
    val holderId: String? = null,
    val bankName: String? = null,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountType? = null,
    val nationalAccountId: String? = null,
) : CreatePaymentAccountPayload
