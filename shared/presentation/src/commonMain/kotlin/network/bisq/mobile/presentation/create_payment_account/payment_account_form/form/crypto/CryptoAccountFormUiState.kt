package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto

import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class CryptoAccountFormUiState(
    val addressEntry: DataEntry = DataEntry(validator = ::validateAddress),
    val isInstant: Boolean = false,
    val isAutoConf: Boolean = false,
    val autoConfNumConfirmationsEntry: DataEntry = DataEntry(validator = ::validateAutoConfNumConfirmations),
    val autoConfMaxTradeAmountEntry: DataEntry = DataEntry(validator = ::validateAutoConfMaxTradeAmount),
    val autoConfExplorerUrlsEntry: DataEntry = DataEntry(validator = ::validateAutoConfExplorerUrls),
)

private const val AUTO_CONF_NUM_CONFIRMATIONS_MIN = 1
private const val AUTO_CONF_NUM_CONFIRMATIONS_MAX = 10
private const val AUTO_CONF_MAX_TRADE_AMOUNT_MIN = 0.001
private const val AUTO_CONF_MAX_TRADE_AMOUNT_MAX = 1.0
private const val AUTO_CONF_EXPLORER_URLS_MIN = 10
private const val AUTO_CONF_EXPLORER_URLS_MAX = 200

internal fun validateAddress(value: String): String? =
    if (value.trim().isBlank()) {
        "validation.empty".i18n()
    } else {
        null
    }

internal fun validateAutoConfNumConfirmations(value: String): String? =
    validateOptionalIntInRange(
        value = value,
        min = AUTO_CONF_NUM_CONFIRMATIONS_MIN,
        max = AUTO_CONF_NUM_CONFIRMATIONS_MAX,
    )

internal fun validateAutoConfMaxTradeAmount(value: String): String? =
    validateOptionalDoubleInRange(
        value = value,
        min = AUTO_CONF_MAX_TRADE_AMOUNT_MIN,
        max = AUTO_CONF_MAX_TRADE_AMOUNT_MAX,
    )

internal fun validateAutoConfExplorerUrls(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) {
        return null
    }

    return if (trimmed.length !in AUTO_CONF_EXPLORER_URLS_MIN..AUTO_CONF_EXPLORER_URLS_MAX) {
        "validation.tooShortOrTooLong".i18n(AUTO_CONF_EXPLORER_URLS_MIN, AUTO_CONF_EXPLORER_URLS_MAX)
    } else {
        null
    }
}

internal fun validateOptionalIntInRange(
    value: String,
    min: Int,
    max: Int,
): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) {
        return null
    }

    val parsed = trimmed.toIntOrNull() ?: return "validation.invalidNumber".i18n()
    return if (parsed !in min..max) {
        "validation.invalidNumber".i18n()
    } else {
        null
    }
}

internal fun validateOptionalDoubleInRange(
    value: String,
    min: Double,
    max: Double,
): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) {
        return null
    }

    val parsed = trimmed.toDoubleOrNull() ?: return "validation.invalidNumber".i18n()
    return if (parsed !in min..max) {
        "validation.invalidNumber".i18n()
    } else {
        null
    }
}
