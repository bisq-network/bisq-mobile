package network.bisq.mobile.domain.service.common

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware

interface LanguageServiceFacade : LifeCycleAware {
    val i18nPairs: StateFlow<List<Pair<String, String>>>

    val allPairs: StateFlow<List<Pair<String, String>>>

    fun setDefaultLanguage(languageCode: String)

    suspend fun sync()
}