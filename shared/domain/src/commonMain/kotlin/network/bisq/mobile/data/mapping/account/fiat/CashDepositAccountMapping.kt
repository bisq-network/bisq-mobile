package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.BankAccountTypeDto
import network.bisq.mobile.data.model.account.fiat.CashDepositAccountDto
import network.bisq.mobile.data.model.account.fiat.CashDepositAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateCashDepositAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateCashDepositAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccountPayload
import network.bisq.mobile.domain.model.account.fiat.BankAccountType
import network.bisq.mobile.domain.model.account.fiat.CashDepositAccount
import network.bisq.mobile.domain.model.account.fiat.CashDepositAccountPayload
import network.bisq.mobile.domain.model.account.fiat.Country

fun CashDepositAccountDto.toDomain(): CashDepositAccount =
    CashDepositAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun CashDepositAccountPayloadDto.toDomain(): CashDepositAccountPayload =
    CashDepositAccountPayload(
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
        currency = currency.toDomain(),
        country = Country(code = country.code, name = country.name),
        holderName = holderName,
        holderId = holderId,
        bankName = bankName,
        bankId = bankId,
        branchId = branchId,
        accountNr = accountNr,
        bankAccountType = bankAccountType?.toDomain(),
        nationalAccountId = nationalAccountId,
        requirements = requirements,
    )

fun CreateCashDepositAccount.toDto(): CreateCashDepositAccountDto =
    CreateCashDepositAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateCashDepositAccountPayload.toDto(): CreateCashDepositAccountPayloadDto =
    CreateCashDepositAccountPayloadDto(
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
        requirements = requirements,
    )

private fun BankAccountTypeDto.toDomain(): BankAccountType = BankAccountType.valueOf(name)

private fun BankAccountType.toDto(): BankAccountTypeDto = BankAccountTypeDto.valueOf(name)
