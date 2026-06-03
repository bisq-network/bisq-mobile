package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer.AchTransferAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer.AchTransferAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer.CreateAchTransferAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer.CreateAchTransferAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType

fun AchTransferAccountDto.toDomain(): AchTransferAccount =
    AchTransferAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun AchTransferAccountPayloadDto.toDomain(): AchTransferAccountPayload =
    AchTransferAccountPayload(
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
        currency = currency.toDomain(),
        country = country.toDomain(),
        holderName = holderName,
        holderAddress = holderAddress,
        bankName = bankName,
        routingNr = routingNr,
        accountNr = accountNr,
        bankAccountType = bankAccountType.toDomain(),
    )

fun CreateAchTransferAccount.toDto(): CreateAchTransferAccountDto =
    CreateAchTransferAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateAchTransferAccountPayload.toDto(): CreateAchTransferAccountPayloadDto =
    CreateAchTransferAccountPayloadDto(
        holderName = holderName,
        holderAddress = holderAddress,
        bankName = bankName,
        routingNr = routingNr,
        accountNr = accountNr,
        bankAccountType = bankAccountType.toDto(),
    )

private fun BankAccountTypeDto.toDomain(): BankAccountType = BankAccountType.valueOf(name)

private fun BankAccountType.toDto(): BankAccountTypeDto = BankAccountTypeDto.valueOf(name)
