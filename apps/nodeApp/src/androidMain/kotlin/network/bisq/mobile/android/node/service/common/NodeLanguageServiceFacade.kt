package network.bisq.mobile.android.node.service.common

import bisq.common.locale.LanguageRepository
import network.bisq.mobile.domain.service.common.LanguageServiceFacade

class NodeLanguageServiceFacade : LanguageServiceFacade() {
    override fun setDefaultLanguage(languageCode: String) {
        LanguageRepository.setDefaultLanguage(languageCode)
    }

    override suspend fun sync() {
        activate()
    }
}