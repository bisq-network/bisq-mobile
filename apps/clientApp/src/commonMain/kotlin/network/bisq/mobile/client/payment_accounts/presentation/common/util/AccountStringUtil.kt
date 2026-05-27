package network.bisq.mobile.client.payment_accounts.presentation.common.util

import network.bisq.mobile.domain.model.account.fiat.BankAccountType
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.i18n.i18n

fun FiatCurrency.toDisplayString(): String = "$code ($name)"

fun BankAccountType.toDisplayString(): String =
    when (this) {
        BankAccountType.CHECKING -> "paymentAccounts.bank.bankAccountType.CHECKINGS".i18n()
        BankAccountType.SAVINGS -> "paymentAccounts.bank.bankAccountType.SAVINGS".i18n()
    }
