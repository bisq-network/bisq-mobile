package network.bisq.mobile.domain.utils

import kotlin.math.pow
import kotlin.math.round

object ByteUnitUtil {
    fun formatBytesPrecise(bytes: Long, decimals: Int = 2): String {
        if (bytes < 1024) return "$bytes B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        // Round to decimals
        val multiplier = 10.0.pow(decimals)
        val rounded = round(value * multiplier) / multiplier

        return "$rounded ${units[unitIndex]}"
    }

}