package network.bisq.mobile.domain.formatters

import network.bisq.mobile.data.utils.decimalFormatter
import network.bisq.mobile.domain.utils.MathUtils.roundTo

object PercentageFormatter {
    // value of 1 means 100%
    fun format(
        value: Double,
        withSymbol: Boolean = true,
    ): String {
        val formatted = decimalFormatter.format(toDisplayedPercent(value), 2)
        return if (withSymbol) "$formatted %" else formatted
    }

    /** The percentage as [format] shows it, before formatting: 0.000_04 gives 0.0. */
    fun toDisplayedPercent(value: Double): Double = (value * 100).roundTo(2)
}
