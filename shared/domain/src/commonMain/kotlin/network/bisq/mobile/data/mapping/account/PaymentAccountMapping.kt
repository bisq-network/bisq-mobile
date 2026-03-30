package network.bisq.mobile.data.mapping.account

import network.bisq.mobile.data.mapping.account.crypto.toDomain
import network.bisq.mobile.data.mapping.account.crypto.toDto
import network.bisq.mobile.data.mapping.account.fiat.toDomain
import network.bisq.mobile.data.mapping.account.fiat.toDto
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.crypto.MoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountDto
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount

fun PaymentAccountDto.toDomain(): PaymentAccount =
    when (this) {
        // Fiat
        is UserDefinedFiatAccountDto -> toDomain()
        is ZelleAccountDto -> toDomain()
        // Crypto
        is MoneroAccountDto -> toDomain()
        is OtherCryptoAssetAccountDto -> toDomain()
        else -> throw IllegalArgumentException("Unsupported PaymentAccountDto type: ${this::class.simpleName}")
    }

fun PaymentAccount.toDto(): PaymentAccountDto =
    when (this) {
        // Fiat
        is UserDefinedFiatAccount -> toDto()
        is ZelleAccount -> toDto()
        // Crypto
        is MoneroAccount -> toDto()
        is OtherCryptoAssetAccount -> toDto()
        else -> throw IllegalArgumentException("Unsupported PaymentAccount type: ${this::class.simpleName}")
    }
