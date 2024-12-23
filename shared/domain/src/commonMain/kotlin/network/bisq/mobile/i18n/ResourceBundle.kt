package network.bisq.mobile.i18n

import kotlinx.datetime.Clock
import network.bisq.mobile.domain.loadProperties
import network.bisq.mobile.utils.getLogger

class ResourceBundle(val bundleName: String, val languageCode: String, val map: Map<String, String>) {
    companion object {
        fun getBundle(bundleName: String, languageCode: String): ResourceBundle {
            var map: Map<String, String>? = null
            try {
                map = loadMappings(bundleName, languageCode)
            } catch (e: Exception) {
                getLogger("ResourceBundle").e("Failed to load bundle i18n for $languageCode", e)
                if (map == null) {
                    getLogger("ResourceBundle").i("Defaulting to english")
                    map = loadMappings(bundleName,"en")
                }
            } finally {
                if (map == null) {
                    throw IllegalArgumentException("Could not find mappings for bundle $bundleName and language $languageCode")
                }
                return ResourceBundle(bundleName, languageCode, map)
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

        // Preprocess content to merge lines ending with '\n\n\'
        private fun preprocessContent(content: String): String {
            val lines = content.lineSequence().toMutableList()
            val mergedLines = mutableListOf<String>()
            var currentLine = ""

            for (line in lines) {
                if (line.endsWith("\\n\\n\\")) {
                    // Remove the trailing '\n\n\' and append it to the current line
                    currentLine += line.removeSuffix("\\n\\n\\")
                } else {
                    // Add the completed current line and reset it
                    currentLine += line
                    mergedLines.add(currentLine)
                    currentLine = ""
                }
            }

            // Add any remaining line
            if (currentLine.isNotEmpty()) {
                mergedLines.add(currentLine)
            }

            return mergedLines.joinToString("\n")
        }
    }

    fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    fun getString(key: String): String {
        return map[key] ?: key
    }
}