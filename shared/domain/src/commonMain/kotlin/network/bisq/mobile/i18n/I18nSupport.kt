package network.bisq.mobile.i18n

class I18nSupport {
    companion object {
        // We use non-printing characters as separator. See: https://en.wikipedia.org/wiki/Delimiter#ASCII_delimited_text
        const val ARGS_SEPARATOR: Char = '\u001F' // ASCII 31
        const val PARAM_SEPARATOR: Char = '\u001E' // ASCII 30


        fun initialize(languageCode: String = "en") {
            // bundles = BUNDLE_NAMES.map { ResourceBundle.getBundle(it, languageCode) }
            val bundleMapsByName: Map<String, Map<String, String>> = when (languageCode) {
                "en" -> GeneratedResourceBundles_en.bundles
                "af-ZA" -> GeneratedResourceBundles_af_ZA.bundles
                "cs" -> GeneratedResourceBundles_cs.bundles
                "de" -> GeneratedResourceBundles_de.bundles
                "es" -> GeneratedResourceBundles_es.bundles
                "it" -> GeneratedResourceBundles_it.bundles
                "pcm" -> GeneratedResourceBundles_pcm.bundles
                "pt-BR" -> GeneratedResourceBundles_pt_BR.bundles
                "ru" -> GeneratedResourceBundles_ru.bundles
                else -> GeneratedResourceBundles_en.bundles
            }
            val maps: Collection<Map<String, String>> = bundleMapsByName.values
            bundles = maps.map { ResourceBundle(it) }
        }

        fun encode(key: String, vararg arguments: Any): String {
            if (arguments.isEmpty()) return key
            val args = arguments.joinToString(ARGS_SEPARATOR.toString())
            return "$key$PARAM_SEPARATOR$args"
        }

        fun decode(encoded: String): String {
            val parts = encoded.split(PARAM_SEPARATOR)
            val key = parts[0]
            val args = parts.getOrNull(1)?.split(ARGS_SEPARATOR)?.toTypedArray() ?: emptyArray()
            return key.i18n(*args)
        }
    }
}

// access with key, e.g.:
// "chat.notifications.privateMessage.headline".i18n() when no no argument is passed
// and: "chat.notifications.offerTaken.message".i18n(1234) with one argument (or more if needed)
fun String.i18n(vararg arguments: Any): String {
    val pattern = i18n()
    val result = MessageFormat.format(pattern, arguments).replace("''", "'")
    return result
}

fun String.i18n(): String {
    val result = bundles
        .firstOrNull { it.containsKey(this) }
        ?.getString(this) ?: "MISSING: [$this]"
    return result
}

lateinit var bundles: List<ResourceBundle>