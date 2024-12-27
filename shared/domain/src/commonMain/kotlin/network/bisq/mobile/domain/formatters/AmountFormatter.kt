package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.decimalFormatter
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.common.monetary.asDouble

object AmountFormatter {
    fun formatRangeAmount(
        minAmount: MonetaryVO,
        maxAmount: MonetaryVO,
        useLowPrecision: Boolean = true,
        withCode: Boolean = true
    ): String {
        return formatAmount(minAmount, useLowPrecision, false) + " - " +
                formatAmount(maxAmount, useLowPrecision, withCode)
    }

    fun formatAmount(amount: MonetaryVO, useLowPrecision: Boolean = true, withCode: Boolean = false): String {
        return format(amount, useLowPrecision) + if (withCode) " ${amount.code}" else ""
    }

    fun format(amount: MonetaryVO, useLowPrecision: Boolean): String {
        val precision = if (useLowPrecision) amount.lowPrecision else amount.precision
        val value = amount.asDouble()
        return decimalFormatter.format(value, precision)
    }
}