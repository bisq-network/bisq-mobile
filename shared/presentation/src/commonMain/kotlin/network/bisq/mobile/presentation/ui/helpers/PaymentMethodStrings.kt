package network.bisq.mobile.presentation.ui.helpers

import network.bisq.mobile.i18n.i18n

// TODO Would be better to find a way to access the string dynamically without custom mapping as that is hard to maintain.
// AI generated mapping
fun i18NPaymentMethod(paymentMethod: String, useShort: Boolean = false): Pair<String, Boolean> {
    val shortVersion = "${paymentMethod}_SHORT"
    val value: String = if (useShort) shortVersion else paymentMethod
    val translated = value.i18n()
    // Todo: Better way to find custom method name?
    return if (translated.startsWith("MISSING: [")) {
        Pair(value, true)
    } else {
        Pair(translated, false)
    }
}
