package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.BankAccountType

data class CreateCashDepositAccountPayload(
    val selectedCountryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String,
    val holderId: String? = null,
    val bankName: String,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountType? = null,
    val nationalAccountId: String? = null,
    val requirements: String? = null,
) : CreatePaymentAccountPayload
