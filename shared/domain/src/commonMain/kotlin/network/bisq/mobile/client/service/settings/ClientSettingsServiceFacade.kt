package network.bisq.mobile.client.service.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientSettingsServiceFacade(val apiGateway: SettingsApiGateway) : SettingsServiceFacade, Logging {
    // Properties
    private val _isTacAccepted: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    override val isTacAccepted: StateFlow<Boolean?> get() = _isTacAccepted

    private val _tradeRulesConfirmed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed


    // API
    override suspend fun getSettings(): Result<SettingsVO> {
        val settingsResult = apiGateway.getSettings()
        if (settingsResult.isSuccess) {
            _isTacAccepted.value = settingsResult.getOrThrow().isTacAccepted
            _tradeRulesConfirmed.value = settingsResult.getOrThrow().tradeRulesConfirmed
        }
        return settingsResult
    }

    override suspend fun confirmTacAccepted(value: Boolean) {
        val result = apiGateway.confirmTacAccepted(value)
        if (result.isSuccess) {
            _isTacAccepted.value = value
        }
    }

    override suspend fun confirmTradeRules(value: Boolean) {
        val result = apiGateway.confirmTradeRules(value)
        if (result.isSuccess) {
            _tradeRulesConfirmed.value = value
        }
    }
}