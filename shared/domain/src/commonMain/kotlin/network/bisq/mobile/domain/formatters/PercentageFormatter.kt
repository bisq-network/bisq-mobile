package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.decimalFormatter
import network.bisq.mobile.domain.utils.MathUtils.roundTo

object PercentageFormatter {

    // value of 1 means 100%
    fun format(value: Double, withSymbol: Boolean = true): String {
        val canonical: Double = (value * 100).roundTo(2)
        val formatted = decimalFormatter.format(canonical, 2)
        return if (withSymbol) "$formatted %" else formatted
    }
}