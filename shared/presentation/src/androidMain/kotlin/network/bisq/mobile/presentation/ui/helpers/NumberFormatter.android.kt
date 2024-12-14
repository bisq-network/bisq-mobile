package network.bisq.mobile.presentation.ui.helpers

import java.text.DecimalFormat

actual val numberFormatter: NumberFormatter = object : NumberFormatter {
    private val formatter = DecimalFormat("0.00000000")
    override fun satsFormat(value: Double): String = formatter.format(value)
}
