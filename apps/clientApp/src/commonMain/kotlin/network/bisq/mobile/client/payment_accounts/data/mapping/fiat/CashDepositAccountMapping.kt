package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CashDepositAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CashDepositAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CreateCashDepositAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CreateCashDepositAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CreateCashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CreateCashDepositAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType

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
        country = country.toDomain(),
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
