package network.bisq.mobile.android.node.service.common

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.common.locale.LanguageRepository
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
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
    private val _i18nCodes: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val i18nCodes: StateFlow<List<String>> get() = _i18nCodes

    // Life cycle
    override fun activate() {
        _i18nCodes.value = LanguageRepository.I18N_CODES
    }

    override fun deactivate() {
    }

}