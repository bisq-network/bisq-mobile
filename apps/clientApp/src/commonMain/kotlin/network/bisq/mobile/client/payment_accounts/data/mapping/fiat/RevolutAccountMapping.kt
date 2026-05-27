package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.CreateRevolutAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.CreateRevolutAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.RevolutAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.RevolutAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccountPayload

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
