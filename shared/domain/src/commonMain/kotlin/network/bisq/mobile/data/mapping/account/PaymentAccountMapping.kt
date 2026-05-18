package network.bisq.mobile.data.mapping.account

import network.bisq.mobile.data.mapping.account.crypto.toDomain
import network.bisq.mobile.data.mapping.account.fiat.toDomain
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.crypto.MoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.WiseAccountDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountDto
import network.bisq.mobile.domain.model.account.PaymentAccount

fun PaymentAccountDto.toDomain(): PaymentAccount =
    when (this) {
        // Fiat
        is UserDefinedFiatAccountDto -> toDomain()
        is ZelleAccountDto -> toDomain()
        is WiseAccountDto -> toDomain()
        // Crypto
        is MoneroAccountDto -> toDomain()
        is OtherCryptoAssetAccountDto -> toDomain()
        else -> throw IllegalArgumentException("Unsupported PaymentAccountDto type: ${this::class.simpleName}")
    }
