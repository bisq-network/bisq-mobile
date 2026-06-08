package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType

data class BankAccountCreatePayloadData(
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
)
