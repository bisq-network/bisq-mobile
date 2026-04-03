package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.ZelleAccountDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountPayloadDto
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

fun ZelleAccount.toDto(): ZelleAccountDto =
    ZelleAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
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

fun ZelleAccountPayload.toDto(): ZelleAccountPayloadDto =
    ZelleAccountPayloadDto(
        holderName = holderName,
        emailOrMobileNr = emailOrMobileNr,
    )
