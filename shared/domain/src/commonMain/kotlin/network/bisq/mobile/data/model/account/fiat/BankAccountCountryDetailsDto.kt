package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class BankAccountCountryDetailsDto(
    val country: CountryDto,
    val holderIdRequired: Boolean,
    val holderIdDescription: String,
    val holderIdDescriptionShort: String,
    val bankAccountTypeRequired: Boolean,
    val bankNameRequired: Boolean,
    val bankIdRequired: Boolean,
    val bankIdDescription: String,
    val bankIdDescriptionShort: String,
    val branchIdRequired: Boolean,
    val branchIdDescription: String,
    val branchIdDescriptionShort: String,
    val accountNrDescription: String,
    val nationalAccountIdRequired: Boolean,
    val nationalAccountIdDescription: String,
    val nationalAccountIdDescriptionShort: String,
)
