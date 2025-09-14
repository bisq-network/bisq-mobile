package network.bisq.mobile.presentation.ui.helpers

import okio.ByteString.Companion.encodeUtf8
import kotlin.math.abs

fun convertToSet(value: String?): Set<String> = value?.let { setOf(it) } ?: emptySet()

fun customPaymentIconIndex(
    customPaymentMethod: String,
    customPaymentIconLength: Int
): Int {
    // 32-byte SHA-256 over the input (multiplatform via Okio)
    val hash = customPaymentMethod.encodeUtf8().sha256().toByteArray()

    // Use the first 4 bytes (big-endian) as an Int
    val b0 = hash[0].toInt() and 0xFF
    val b1 = hash[1].toInt() and 0xFF
    val b2 = hash[2].toInt() and 0xFF
    val b3 = hash[3].toInt() and 0xFF
    val raw = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3

    // Normalize to non-negative (guard against Int.MIN_VALUE)
    val nonNegative = if (raw == Int.MIN_VALUE) 0 else abs(raw)

    return nonNegative % customPaymentIconLength + 1
}
