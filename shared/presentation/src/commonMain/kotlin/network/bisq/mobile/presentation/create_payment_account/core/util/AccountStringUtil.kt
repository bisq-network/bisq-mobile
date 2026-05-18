package network.bisq.mobile.presentation.create_payment_account.core.util

fun fiatCurrencyCodeNameToDisplayStringFormat(
    code: String,
    name: String,
): String = "$code ($name)"
