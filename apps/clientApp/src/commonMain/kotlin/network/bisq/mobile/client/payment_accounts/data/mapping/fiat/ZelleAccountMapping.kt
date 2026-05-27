package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.CreateZelleAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.CreateZelleAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.ZelleAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.ZelleAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccountPayload

fun ZelleAccountDto.toDomain(): ZelleAccount =
    ZelleAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun ZelleAccountPayloadDto.toDomain(): ZelleAccountPayload =
    ZelleAccountPayload(
        holderName = holderName,
        emailOrMobileNr = emailOrMobileNr,
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
        currency = FiatCurrency(code = currency, name = currency),
        country = Country(code = country, name = country),
    )

fun CreateZelleAccount.toDto(): CreateZelleAccountDto =
    CreateZelleAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateZelleAccountPayload.toDto(): CreateZelleAccountPayloadDto =
    CreateZelleAccountPayloadDto(
        holderName = holderName,
        emailOrMobileNr = emailOrMobileNr,
    )
