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

        // To keep "en" the only language for language lists
        val langCode = LanguageRepository.getDefaultLanguage() ?: "en"
        LanguageRepository.setDefaultLanguage("en")

        val displayTextList = mutableListOf<String>()
        for (code in LanguageRepository.I18N_CODES) {
            displayTextList.add(LanguageRepository.getDisplayString(code))
        }
        _i18nPairs.value = LanguageRepository.I18N_CODES.zip(displayTextList)

        displayTextList.clear()
        for (code in LanguageRepository.CODES) {
            displayTextList.add(LanguageRepository.getDisplayString(code))
        }
        _allPairs.value = LanguageRepository.CODES.zip(displayTextList)

        LanguageRepository.setDefaultLanguage(langCode)
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