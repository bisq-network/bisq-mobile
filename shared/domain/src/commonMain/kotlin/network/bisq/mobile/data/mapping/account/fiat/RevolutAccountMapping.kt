package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.RevolutAccountDto
import network.bisq.mobile.data.model.account.fiat.RevolutAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateRevolutAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateRevolutAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccountPayload
import network.bisq.mobile.domain.model.account.fiat.RevolutAccount
import network.bisq.mobile.domain.model.account.fiat.RevolutAccountPayload

fun RevolutAccountDto.toDomain(): RevolutAccount =
    RevolutAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun RevolutAccountPayloadDto.toDomain(): RevolutAccountPayload =
    RevolutAccountPayload(
        selectedCurrencies = selectedCurrencies.map { currency -> currency.toDomain() },
        userName = userName,
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
    )

fun CreateRevolutAccount.toDto(): CreateRevolutAccountDto =
    CreateRevolutAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateRevolutAccountPayload.toDto(): CreateRevolutAccountPayloadDto =
    CreateRevolutAccountPayloadDto(
        userName = userName,
        selectedCurrencyCodes = selectedCurrencies.map { currency -> currency.code },
    )
