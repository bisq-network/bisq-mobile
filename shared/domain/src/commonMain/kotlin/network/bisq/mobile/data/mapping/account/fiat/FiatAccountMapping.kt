package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.domain.model.account.fiat.FiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount

fun FiatAccountDto.toDomain(): FiatAccount =
    when (this) {
        is UserDefinedFiatAccountDto -> toDomain()
        else -> throw IllegalArgumentException("Unsupported FiatAccountDto type: ${this::class.simpleName}")
    }

fun FiatAccount.toDto(): FiatAccountDto =
    when (this) {
        is UserDefinedFiatAccount -> toDto()
        else -> throw IllegalArgumentException("Unsupported FiatAccount type: ${this::class.simpleName}")
    }
