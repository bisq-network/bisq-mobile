package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountCountryDetailsDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails

fun BankAccountCountryDetailsDto.toDomain(): BankAccountCountryDetails =
    BankAccountCountryDetails(
        country = country.toDomain(),
        bankAccountValidationSupported = bankAccountValidationSupported,
        holderIdRequired = holderIdRequired,
        holderIdDescription = holderIdDescription,
        holderIdDescriptionShort = holderIdDescriptionShort,
        bankAccountTypeRequired = bankAccountTypeRequired,
        bankNameRequired = bankNameRequired,
        bankIdRequired = bankIdRequired,
        bankIdDescription = bankIdDescription,
        bankIdDescriptionShort = bankIdDescriptionShort,
        branchIdRequired = branchIdRequired,
        branchIdDescription = branchIdDescription,
        branchIdDescriptionShort = branchIdDescriptionShort,
        accountNrDescription = accountNrDescription,
        nationalAccountIdRequired = nationalAccountIdRequired,
        nationalAccountIdDescription = nationalAccountIdDescription,
        nationalAccountIdDescriptionShort = nationalAccountIdDescriptionShort,
    )
