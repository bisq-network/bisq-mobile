package network.bisq.mobile.client.common.domain.service.common

import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.service.common.LanguageServiceFacade

class ClientLanguageServiceFacade : LanguageServiceFacade() {

    private val _defaultLanguage: MutableStateFlow<String> = MutableStateFlow("en")

    override fun setDefaultLanguage(languageCode: String) {
        _defaultLanguage.value = languageCode
    }

    override suspend fun sync() {
        activate()
    }
}