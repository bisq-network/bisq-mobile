package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa.CreateSepaAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa.CreateSepaAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa.SepaAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa.SepaAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.SepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.SepaAccountPayload

fun SepaAccountDto.toDomain(): SepaAccount =
    SepaAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun SepaAccountPayloadDto.toDomain(): SepaAccountPayload =
    SepaAccountPayload(
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
        currency = currency.toDomain(),
        country = country.toDomain(),
        acceptedCountries = acceptedCountries.map { acceptedCountry -> acceptedCountry.toDomain() },
        holderName = holderName,
        iban = iban,
        bic = bic,
    )

fun CreateSepaAccount.toDto(): CreateSepaAccountDto =
    CreateSepaAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateSepaAccountPayload.toDto(): CreateSepaAccountPayloadDto =
    CreateSepaAccountPayloadDto(
        selectedCountryCode = selectedCountryCode,
        acceptedCountryCodes = acceptedCountryCodes,
        holderName = holderName,
        iban = iban,
        bic = bic,
    )
