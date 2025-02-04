package network.bisq.mobile.android.node.service.common

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.common.locale.LanguageRepository
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.zip
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeLanguageServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    LanguageServiceFacade, Logging {

    // Dependencies
    private val languageService: LanguageRepository by lazy {
        applicationService.languageRepository.get()
    }

    // Properties
    private val _i18nPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val i18nPairs: StateFlow<List<Pair<String, String>>> = _i18nPairs

    private val _allPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val allPairs: StateFlow<List<Pair<String, String>>> = _allPairs

    override fun setDefaultLanguage(languageCode: String) {
        return LanguageRepository.setDefaultLanguage(languageCode)
    }

    // Life cycle
    override fun activate() {

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

    }

    override suspend fun sync() {
        activate()
    }

    override fun deactivate() {
    }

}