package network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country

data class BankAccountCountryDetails(
    val country: Country,
    val bankAccountValidationSupported: Boolean,
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
