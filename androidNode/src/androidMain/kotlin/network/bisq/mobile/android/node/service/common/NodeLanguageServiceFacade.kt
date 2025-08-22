package network.bisq.mobile.android.node.service.common

import bisq.common.locale.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade

class NodeLanguageServiceFacade : ServiceFacade(), LanguageServiceFacade {

    // Properties
    private val _i18nPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val i18nPairs: StateFlow<List<Pair<String, String>>> get() = _i18nPairs.asStateFlow()

    private val _allPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val allPairs: StateFlow<List<Pair<String, String>>> get() = _allPairs.asStateFlow()

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()

        // Use consistent language codes that match NodeSettingsServiceFacade
        _i18nPairs.value = listOf(
            "af-ZA" to "Afrikaans (Afrikaans)",
            "cs" to "Czech (\u010de\u0161tina)",
            "de" to "German (Deutsch)",
            "en" to "English",
            "es" to "Spanish (Espa\u00f1ol)",
            "it" to "Italian (Italiano)",
            "pcm" to "Pidgin (Naij\u00e1)",
            "pt-BR" to "Portuguese (Portugu\u00eas)",
            "ru" to "Russian (\u0440\u0443\u0441\u0441\u043a\u0438\u0439)"
        )

        _allPairs.value = listOf(
            "af-ZA" to "Afrikaans (Afrikaans)",
            "cs" to "Czech (\u010de\u0161tina)",
            "de" to "German (Deutsch)",
            "en" to "English",
            "es" to "Spanish (espa\u00f1ol)",
            "it" to "Italian (italiano)",
            "pcm" to "Pidgin (Naij\u00e1)",
            "pt-BR" to "Portuguese (portugu\u00eas)",
            "ru" to "Russian (\u0440\u0443\u0441\u0441\u043a\u0438\u0439)"
        )
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override fun setDefaultLanguage(languageCode: String) {
        LanguageRepository.setDefaultLanguage(languageCode)
    }

    override suspend fun sync() {
        activate()
    }
}