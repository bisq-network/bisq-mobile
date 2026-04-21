package network.bisq.mobile.domain.formatters

import network.bisq.mobile.data.utils.decimalFormatter
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

    /** Matches bisq2 desktop CSV: no grouping separators. */
    fun formatForCsvExport(value: Double): String {
        val canonical = value.roundTo(2)
        return decimalFormatter.format(canonical, 2, useGrouping = false)
    }

    fun btcFormatForCsvExport(value: Long): String {
        val fraction = value / 100_000_000.0
        return decimalFormatter.format(fraction, 8, useGrouping = false)
    }
}
