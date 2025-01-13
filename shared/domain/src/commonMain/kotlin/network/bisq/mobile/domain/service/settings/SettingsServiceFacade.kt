package network.bisq.mobile.domain.service.settings

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO

interface SettingsServiceFacade : LifeCycleAware {
    val isTacAccepted: StateFlow<Boolean?>
    val tradeRulesConfirmed: StateFlow<Boolean>

    suspend fun getSettings(): Result<SettingsVO>
    suspend fun confirmTacAccepted(value: Boolean)
    suspend fun confirmTradeRules(value: Boolean)
}