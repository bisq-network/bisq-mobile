package network.bisq.mobile.android.node.service.settings

import bisq.common.observable.Pin
import bisq.settings.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeSettingsServiceFacade(applicationService: AndroidApplicationService.Provider) : SettingsServiceFacade, Logging {
    // Dependencies
    private val settingsService: SettingsService by lazy { applicationService.settingsService.get() }

    // Properties
    private val _isTacAccepted: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    override val isTacAccepted: StateFlow<Boolean?> get() = _isTacAccepted

    private val _tradeRulesConfirmed = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed


    // Misc
    private var tradeRulesConfirmedPin: Pin? = null


    override fun activate() {
        tradeRulesConfirmedPin = settingsService.isTacAccepted.addObserver { isTacAccepted ->
            _isTacAccepted.value = isTacAccepted
        }
        tradeRulesConfirmedPin = settingsService.tradeRulesConfirmed.addObserver { isConfirmed ->
            _tradeRulesConfirmed.value = isConfirmed
        }
    }

    override fun deactivate() {
        tradeRulesConfirmedPin?.unbind()
    }

    // API
    override suspend fun getSettings(): Result<SettingsVO> {
        return Result.success(Mappings.SettingsMapping.from(settingsService))
    }

    override suspend fun confirmTacAccepted(value: Boolean) {
        settingsService.isTacAccepted.set(value)
    }

    override suspend fun confirmTradeRules(value: Boolean) {
        settingsService.tradeRulesConfirmed.set(value)
    }
}