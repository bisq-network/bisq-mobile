package network.bisq.mobile.client.payment_accounts.data.mapping

import network.bisq.mobile.client.payment_accounts.data.mapping.crypto.toDto
import network.bisq.mobile.client.payment_accounts.data.mapping.fiat.toDto
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CreateCashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount

fun CreatePaymentAccount.toDto(): CreatePaymentAccountDto =
    when (this) {
        is CreateUserDefinedFiatAccount -> toDto()
        is CreateAchTransferAccount -> toDto()
        is CreateCashDepositAccount -> toDto()
        is CreateZelleAccount -> toDto()
        is CreateWiseAccount -> toDto()
        is CreateRevolutAccount -> toDto()
        is CreateSepaAccount -> toDto()
        is CreateMoneroAccount -> toDto()
        is CreateOtherCryptoAssetAccount -> toDto()
        else -> error("Unsupported create payment account type: ${this::class.simpleName}")
    }
