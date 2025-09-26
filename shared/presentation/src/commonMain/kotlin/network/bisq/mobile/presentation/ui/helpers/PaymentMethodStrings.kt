package network.bisq.mobile.presentation.ui.helpers

import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n

// TODO Would be better to find a way to access the string dynamically without custom mapping as that is hard to maintain.
// AI generated mapping
fun i18NPaymentMethod(paymentMethodKey: String, useShort: Boolean = false): Pair<String, Boolean> {
    val shortVersion = "${paymentMethodKey}_SHORT"
    val key: String = if (useShort) shortVersion else paymentMethodKey
    val hasEntry = I18nSupport.has(key)
    val value = if (hasEntry) key.i18n() else key
    return Pair(value, !hasEntry)
}
