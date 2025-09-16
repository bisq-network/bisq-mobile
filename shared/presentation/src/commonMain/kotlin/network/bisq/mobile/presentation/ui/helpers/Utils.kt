package network.bisq.mobile.presentation.ui.helpers

import okio.ByteString.Companion.encodeUtf8
import kotlin.math.abs

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
