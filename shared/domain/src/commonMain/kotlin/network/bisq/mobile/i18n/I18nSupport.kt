package network.bisq.mobile.i18n

class I18nSupport {
    companion object {
        fun initialize(languageCode: String) {
            bundles = BUNDLE_NAMES.map { ResourceBundle.getBundle(it, languageCode) }
        }
    }
}

// access with key, e.g.:
// "chat.notifications.privateMessage.headline".i18n() when no no argument is passed
// and: "chat.notifications.offerTaken.message".i18n(1234) with one argument (or more if needed)
fun String.i18n(vararg arguments: Any): String {
    val pattern = i18n()
    return MessageFormat.format(pattern, arguments)
}

fun String.i18n(): String {
    val result = bundles
        .firstOrNull { it.containsKey(this) }
        ?.getString(this) ?: "MISSING: [$this]"
    return result
}

lateinit var bundles: List<ResourceBundle>
private val BUNDLE_NAMES: List<String> = listOf(
    "default",
    "application",
    "bisq_easy",
    "reputation",
    // "trade_apps", // Not used
    // "academy", // Not used
    "chat",
    "support",
    "user",
    "network",
    "settings",
    // "wallet", // Not used
    // "authorized_role", // Not used
    "payment_method",
    "offer",
    "mobile" // custom for mobile client
)