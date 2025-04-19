package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.decimalFormatter
import network.bisq.mobile.domain.utils.MathUtils.roundTo

object NumberFormatter {
    fun format(value: Double): String {
        val canonical: Double = value.roundTo(2)
        val formatted = decimalFormatter.format(canonical, 2)
        return formatted
    }

    fun btcFormat(value: Long): String {
        val fraction = value / 100_000_000.0
        val formatted = decimalFormatter.format(fraction, 8)
        return formatted
    }
}