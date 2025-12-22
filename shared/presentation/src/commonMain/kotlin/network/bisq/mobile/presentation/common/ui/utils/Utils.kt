package network.bisq.mobile.presentation.common.ui.utils

import okio.ByteString.Companion.encodeUtf8
import kotlin.math.abs

const val EMPTY_STRING = ""

fun convertToSet(value: String?): Set<String> = value?.let { setOf(it) } ?: emptySet()

fun customPaymentIconIndex(
    customPaymentMethod: String,
    customPaymentIconLength: Int
): Int {
    require(customPaymentIconLength > 0) { "customPaymentIconLength must be > 0" }

    // 32-byte SHA-256 over the input (multiplatform via Okio)
    val hash = customPaymentMethod.encodeUtf8().sha256().toByteArray()

    val i =
        ((hash[28].toInt() and 0xFF) shl 24) or
                ((hash[29].toInt() and 0xFF) shl 16) or
                ((hash[30].toInt() and 0xFF) shl 8)  or
                ( hash[31].toInt() and 0xFF)

    val nonNegative = if (i == Int.MIN_VALUE) 0 else abs(i)

    return nonNegative % customPaymentIconLength
}

// Payment methods that have dedicated icon files in drawable/payment/fiat/
private val KNOWN_FIAT_PAYMENT_ICONS = setOf(
    "ACH_TRANSFER",
    "AMAZON_GIFT_CARD",
    "BIZUM",
    "CASH_APP",
    "CASH_BY_MAIL",
    "CASH_DEPOSIT",
    "F2F",
    "FASTER_PAYMENTS",
    "INTERAC_E_TRANSFER",
    "NATIONAL_BANK",
    "PAY_ID",
    "PIX",
    "REVOLUT",
    "SEPA",
    "SEPA_INSTANT",
    "STRIKE",
    "SWIFT",
    "UPI",
    "US_POSTAL_MONEY_ORDER",
    "WISE",
    "ZELLE",
)

// Settlement methods that have dedicated icon files in drawable/payment/bitcoin/
private val KNOWN_SETTLEMENT_ICONS = setOf(
    "BTC",
    "MAIN_CHAIN",
    "ONCHAIN",
    "ON_CHAIN",
    "LIGHTNING",
    "LN",
)

/**
 * Returns true if the payment method ID has a known icon file.
 * This is used to determine whether to show a custom fallback icon.
 */
fun hasKnownPaymentIcon(paymentMethodId: String): Boolean =
    KNOWN_FIAT_PAYMENT_ICONS.contains(paymentMethodId.uppercase())

/**
 * Returns true if the settlement method ID has a known icon file.
 */
fun hasKnownSettlementIcon(settlementMethodId: String): Boolean =
    KNOWN_SETTLEMENT_ICONS.contains(settlementMethodId.uppercase())
