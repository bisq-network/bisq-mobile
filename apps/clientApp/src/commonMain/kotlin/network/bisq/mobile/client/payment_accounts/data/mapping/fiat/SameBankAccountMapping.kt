package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.same_bank.CreateSameBankAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.same_bank.CreateSameBankAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.same_bank.SameBankAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.same_bank.SameBankAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.CreateSameBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.CreateSameBankAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccountPayload

fun SameBankAccountDto.toDomain(): SameBankAccount =
    SameBankAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun SameBankAccountPayloadDto.toDomain(): SameBankAccountPayload =
    SameBankAccountPayload(
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

fun CreateSameBankAccount.toDto(): CreateSameBankAccountDto =
    CreateSameBankAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateSameBankAccountPayload.toDto(): CreateSameBankAccountPayloadDto =
    CreateSameBankAccountPayloadDto(
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
