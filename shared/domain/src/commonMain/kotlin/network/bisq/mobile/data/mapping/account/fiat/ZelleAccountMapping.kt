package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateZelleAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateZelleAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccountPayload
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload

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
        currency = currency,
        country = country,
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
