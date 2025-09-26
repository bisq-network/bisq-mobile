package network.bisq.mobile.presentation.ui.helpers

import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n

// TODO Would be better to find a way to access the string dynamically without custom mapping as that is hard to maintain.
// AI generated mapping
fun i18NPaymentMethod(paymentMethodKey: String, useShort: Boolean = false): Pair<String, Boolean> {
    if (useShort) {
        val shortKey = "${paymentMethodKey}_SHORT"
        if (I18nSupport.has(shortKey)) {
            return shortKey.i18n() to false
        }
        val (fallbackValue, fallbackMissing) = i18NPaymentMethod(paymentMethodKey)
        return fallbackValue to fallbackMissing
    }

    val hasEntry = I18nSupport.has(paymentMethodKey)
    val value = if (hasEntry) paymentMethodKey.i18n() else paymentMethodKey
    return value to !hasEntry
}
