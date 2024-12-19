package network.bisq.mobile.presentation.ui.helpers

interface NumberFormatter {
    // TODO: Should be re-named eightDecimalFormat
    fun satsFormat(value: Double): String
}

expect val numberFormatter: NumberFormatter
