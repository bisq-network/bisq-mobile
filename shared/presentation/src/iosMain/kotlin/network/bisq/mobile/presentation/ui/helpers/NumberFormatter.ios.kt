package network.bisq.mobile.presentation.ui.helpers

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual val numberFormatter: NumberFormatter = object : NumberFormatter {
    override fun satsFormat(value: Double): String =
        NSString.stringWithFormat("%.8f", value)
}
