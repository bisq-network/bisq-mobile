package network.bisq.mobile.presentation.ui.helpers

interface NumberFormatter {
    fun satsFormat(value: Double): String
}

expect val numberFormatter: NumberFormatter
