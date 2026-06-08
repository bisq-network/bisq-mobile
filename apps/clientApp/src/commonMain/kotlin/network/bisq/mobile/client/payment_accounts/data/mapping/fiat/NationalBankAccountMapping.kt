package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.CreateNationalBankAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.CreateNationalBankAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.NationalBankAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.NationalBankAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.NationalBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.NationalBankAccountPayload

fun NationalBankAccountDto.toDomain(): NationalBankAccount =
    NationalBankAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun NationalBankAccountPayloadDto.toDomain(): NationalBankAccountPayload =
    NationalBankAccountPayload(
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
        currency = currency.toDomain(),
        country = country.toDomain(),
        holderName = holderName,
        holderId = holderId,
        bankName = bankName,
        bankId = bankId,
        branchId = branchId,
        accountNr = accountNr,
        bankAccountType = bankAccountType?.toDomain(),
        nationalAccountId = nationalAccountId,
    )

fun CreateNationalBankAccount.toDto(): CreateNationalBankAccountDto =
    CreateNationalBankAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateNationalBankAccountPayload.toDto(): CreateNationalBankAccountPayloadDto =
    CreateNationalBankAccountPayloadDto(
        selectedCountryCode = selectedCountryCode,
        selectedCurrencyCode = selectedCurrencyCode,
        holderName = holderName,
        holderId = holderId,
        bankName = bankName,
        bankId = bankId,
        branchId = branchId,
        accountNr = accountNr,
        bankAccountType = bankAccountType?.toDto(),
        nationalAccountId = nationalAccountId,
    )
