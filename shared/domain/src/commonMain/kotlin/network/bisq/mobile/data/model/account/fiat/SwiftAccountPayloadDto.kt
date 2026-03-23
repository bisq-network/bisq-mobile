package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SwiftAccountPayloadDto(
    val bankCountryCode: String,
    val beneficiaryName: String,
    val beneficiaryAccountNr: String,
    val beneficiaryPhone: String? = null,
    val beneficiaryAddress: String,
    val selectedCurrencyCode: String,
    val bankSwiftCode: String,
    val bankName: String,
    val bankBranch: String? = null,
    val bankAddress: String,
    val intermediaryBankCountryCode: String? = null,
    val intermediaryBankSwiftCode: String? = null,
    val intermediaryBankName: String? = null,
    val intermediaryBankBranch: String? = null,
    val intermediaryBankAddress: String? = null,
    val additionalInstructions: String? = null,
) : FiatAccountPayloadDto
