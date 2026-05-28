package network.bisq.mobile.data.mapping.account

import network.bisq.mobile.data.mapping.account.crypto.toDto
import network.bisq.mobile.data.mapping.account.fiat.toDto
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount

fun CreatePaymentAccount.toDto(): CreatePaymentAccountDto =
    when (this) {
        is CreateUserDefinedFiatAccount -> toDto()
        is CreateCashDepositAccount -> toDto()
        is CreateZelleAccount -> toDto()
        is CreateWiseAccount -> toDto()
        is CreateRevolutAccount -> toDto()
        is CreateMoneroAccount -> toDto()
        is CreateOtherCryptoAssetAccount -> toDto()
        else -> error("Unsupported create payment account type: ${this::class.simpleName}")
    }
