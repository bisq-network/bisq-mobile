package network.bisq.mobile.client.payment_accounts.data.mapping

import network.bisq.mobile.client.payment_accounts.data.mapping.crypto.toDomain
import network.bisq.mobile.client.payment_accounts.data.mapping.fiat.toDomain
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.MoneroAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CashDepositAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.RevolutAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined.UserDefinedFiatAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.WiseAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.ZelleAccountDto
import network.bisq.mobile.domain.model.account.PaymentAccount

fun PaymentAccountDto.toDomain(): PaymentAccount =
    when (this) {
        // Fiat
        is UserDefinedFiatAccountDto -> toDomain()
        is CashDepositAccountDto -> toDomain()
        is ZelleAccountDto -> toDomain()
        is WiseAccountDto -> toDomain()
        is RevolutAccountDto -> toDomain()
        // Crypto
        is MoneroAccountDto -> toDomain()
        is OtherCryptoAssetAccountDto -> toDomain()
        else -> throw IllegalArgumentException("Unsupported PaymentAccountDto type: ${this::class.simpleName}")
    }
