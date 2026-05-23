package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.WiseAccountDto
import network.bisq.mobile.data.model.account.fiat.WiseAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateWiseAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateWiseAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccountPayload
import network.bisq.mobile.domain.model.account.fiat.WiseAccount
import network.bisq.mobile.domain.model.account.fiat.WiseAccountPayload

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
