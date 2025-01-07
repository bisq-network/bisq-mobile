package network.bisq.mobile.i18n

class I18nSupport {
    companion object {
        fun initialize(languageCode: String = "en") {
            // bundles = BUNDLE_NAMES.map { ResourceBundle.getBundle(it, languageCode) }
            val bundleMapsByName: Map<String, Map<String, String>> = when (languageCode) {
                "en" -> GeneratedResourceBundles_en.bundles
                "af_ZA" -> GeneratedResourceBundles_af_ZA.bundles
                "cs" -> GeneratedResourceBundles_cs.bundles
                "de" -> GeneratedResourceBundles_de.bundles
                "es" -> GeneratedResourceBundles_es.bundles
                "it" -> GeneratedResourceBundles_it.bundles
                "pcm" -> GeneratedResourceBundles_pcm.bundles
                "pt_BR" -> GeneratedResourceBundles_pt_BR.bundles
                "ru" -> GeneratedResourceBundles_ru.bundles
                else -> GeneratedResourceBundles_en.bundles
            }
            val maps: Collection<Map<String, String>> = bundleMapsByName.values
            bundles = maps.map { ResourceBundle(it) }
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