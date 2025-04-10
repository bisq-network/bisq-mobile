package network.bisq.mobile.presentation.ui.helpers

actual val numberFormatter: NumberFormatter = object : NumberFormatter {
    override fun satsFormat(value: Double): String {
        // Format with 8 decimal places
        return value.toFixed(8)
    }
}

// Extension function to format a double with fixed decimal places in JS
private fun Double.toFixed(digits: Int): String {
    // In JS, we can use the native toFixed method
    return this.asDynamic().toFixed(digits) as String
}