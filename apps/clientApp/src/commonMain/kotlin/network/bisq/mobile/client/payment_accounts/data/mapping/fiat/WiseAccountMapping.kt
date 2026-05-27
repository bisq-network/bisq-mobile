package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.CreateWiseAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.CreateWiseAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.WiseAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.WiseAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccountPayload

fun WiseAccountDto.toDomain(): WiseAccount =
    WiseAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun WiseAccountPayloadDto.toDomain(): WiseAccountPayload =
    WiseAccountPayload(
        selectedCurrencies = selectedCurrencies.map { currency -> currency.toDomain() },
        holderName = holderName,
        email = email,
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
    )

fun CreateWiseAccount.toDto(): CreateWiseAccountDto =
    CreateWiseAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateWiseAccountPayload.toDto(): CreateWiseAccountPayloadDto =
    CreateWiseAccountPayloadDto(
        selectedCurrencyCodes = selectedCurrencies.map { currency -> currency.code },
        holderName = holderName,
        email = email,
    )
