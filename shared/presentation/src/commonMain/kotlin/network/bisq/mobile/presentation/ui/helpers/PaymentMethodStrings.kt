package network.bisq.mobile.presentation.ui.helpers

import network.bisq.mobile.i18n.i18n

// TODO Would be better to find a way to access the string dynamically without custom mapping as that is hard to maintain.
// AI generated mapping
fun i18NPaymentMethod(paymentMethod: String, useShort: Boolean = false): String {
    val shortVersion = "${paymentMethod}_SHORT"
    val value: String = if (useShort) shortVersion else paymentMethod
    return value.i18n()
}
