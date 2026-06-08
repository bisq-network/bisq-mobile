package network.bisq.mobile.client.payment_accounts.data.model.fiat.same_bank

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto

@Serializable
data class CreateSameBankAccountPayloadDto(
    val selectedCountryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String? = null,
    val holderId: String? = null,
    val bankName: String? = null,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountTypeDto? = null,
    val nationalAccountId: String? = null,
) : CreatePaymentAccountPayloadDto
