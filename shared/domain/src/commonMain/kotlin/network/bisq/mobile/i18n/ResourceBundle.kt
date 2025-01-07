package network.bisq.mobile.i18n

import kotlinx.datetime.Clock
import network.bisq.mobile.domain.loadProperties
import network.bisq.mobile.domain.utils.getLogger

class ResourceBundle(val map: Map<String, String>) {
    companion object {
        fun getBundle(bundleName: String, languageCode: String): ResourceBundle {
            var map: Map<String, String>? = null
            try {
                map = loadMappings(bundleName, languageCode)
            } catch (e: Exception) {
                getLogger("ResourceBundle").e("Failed to load bundle i18n for $languageCode", e)
                if (map == null) {
                    getLogger("ResourceBundle").i("Defaulting to english")
                    map = loadMappings(bundleName, "en")
                }
            } finally {
                if (map == null) {
                    throw IllegalArgumentException("Could not find mappings for bundle $bundleName and language $languageCode")
                }
                return ResourceBundle(map)
            }
        }

        private fun loadMappings(bundleName: String, languageCode: String): Map<String, String> {
            val code = if (languageCode.lowercase() == "en") "" else "_$languageCode"
            // We must use a sub directory as otherwise it would get shadowed with the resources from bisq 2 i18n jar in node
            val fileName = "mobile/$bundleName$code.properties"
            val ts = Clock.System.now()
            getLogger("ResourceBundle").i("Loading $bundleName took ${Clock.System.now() - ts}")
            return loadProperties(fileName)
        }
    }

    fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    fun getString(key: String): String {
        return map[key] ?: key
    }
}