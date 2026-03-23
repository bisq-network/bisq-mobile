package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class CashDepositAccountPayloadDto(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val holderName: String? = null,
    val holderId: String? = null,
    val bankName: String? = null,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountTypeDto? = null,
    val nationalAccountId: String? = null,
    val requirements: String? = null,
) : FiatAccountPayloadDto
