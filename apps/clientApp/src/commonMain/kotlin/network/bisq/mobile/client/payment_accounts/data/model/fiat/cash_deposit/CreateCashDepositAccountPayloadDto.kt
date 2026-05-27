package network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto

@Serializable
data class CreateCashDepositAccountPayloadDto(
    val selectedCountryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String,
    val holderId: String? = null,
    val bankName: String,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountTypeDto? = null,
    val nationalAccountId: String? = null,
    val requirements: String? = null,
) : CreatePaymentAccountPayloadDto
