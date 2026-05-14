package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateUserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateUserDefinedFiatAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccountPayload
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload

fun UserDefinedFiatAccountDto.toDomain(): UserDefinedFiatAccount =
    UserDefinedFiatAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun UserDefinedFiatAccountPayloadDto.toDomain(): UserDefinedFiatAccountPayload =
    UserDefinedFiatAccountPayload(
        accountData = accountData,
        chargebackRisk = chargebackRisk?.toDomain(),
        paymentMethodName = paymentMethodName,
    )

fun CreateUserDefinedFiatAccount.toDto(): CreateUserDefinedFiatAccountDto =
    CreateUserDefinedFiatAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateUserDefinedFiatAccountPayload.toDto(): CreateUserDefinedFiatAccountPayloadDto =
    CreateUserDefinedFiatAccountPayloadDto(
        accountData = accountData,
    )
