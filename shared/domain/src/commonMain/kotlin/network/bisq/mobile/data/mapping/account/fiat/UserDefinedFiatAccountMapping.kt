package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountPayloadDto
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload

fun UserDefinedFiatAccountDto.toDomain(): UserDefinedFiatAccount =
    UserDefinedFiatAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
    )

fun UserDefinedFiatAccount.toDto(): UserDefinedFiatAccountDto =
    UserDefinedFiatAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
        creationDate = creationDate,
    )

fun UserDefinedFiatAccountPayloadDto.toDomain(): UserDefinedFiatAccountPayload =
    UserDefinedFiatAccountPayload(
        accountData = accountData,
    )

fun UserDefinedFiatAccountPayload.toDto(): UserDefinedFiatAccountPayloadDto =
    UserDefinedFiatAccountPayloadDto(
        accountData = accountData,
    )
