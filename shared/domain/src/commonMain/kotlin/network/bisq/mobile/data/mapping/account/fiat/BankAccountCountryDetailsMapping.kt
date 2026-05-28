package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.BankAccountCountryDetailsDto
import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails
import network.bisq.mobile.domain.model.account.fiat.Country

fun BankAccountCountryDetailsDto.toDomain(): BankAccountCountryDetails =
    BankAccountCountryDetails(
        country = Country(code = country.code, name = country.name),
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
