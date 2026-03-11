package network.bisq.mobile.client.common.domain.service.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.I18nSupport

class ClientSettingsServiceFacade(
    private val apiGateway: SettingsApiGateway,
) : ServiceFacade(),
    SettingsServiceFacade,
    Logging {
    override suspend fun confirmTacAccepted(value: Boolean): Result<Unit> = apiGateway.confirmTacAccepted(value)

    private val _tradeRulesConfirmed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed.asStateFlow()

    override suspend fun confirmTradeRules(value: Boolean): Result<Unit> =
        apiGateway
            .confirmTradeRules(value)
            .onSuccess {
                _tradeRulesConfirmed.value = value
            }

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("")
    override val languageCode: StateFlow<String> get() = _languageCode.asStateFlow()

    override suspend fun setLanguageCode(value: String): Result<Unit> {
        try {
            log.i { "Client attempting to set language code to: $value" }
            val result = apiGateway.setLanguageCode(value)
            if (result.isSuccess) {
                updateLanguage(value)
                log.i { "Client successfully set language code to: $value (via API)" }
            } else {
                log.e { "Client API call failed for language code: $value" }
            }
            return result
        } catch (e: Exception) {
            log.e(e) { "Client failed to set language code to: $value" }
            return Result.failure(e)
        }
    }

    override suspend fun setSupportedLanguageCodes(value: Set<String>): Result<Unit> = apiGateway.setSupportedLanguageCodes(value)

    override suspend fun setCloseMyOfferWhenTaken(value: Boolean): Result<Unit> = apiGateway.setCloseMyOfferWhenTaken(value)

    override suspend fun setMaxTradePriceDeviation(value: Double): Result<Unit> = apiGateway.setMaxTradePriceDeviation(value)

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> get() = _useAnimations.asStateFlow()

    override suspend fun setUseAnimations(value: Boolean): Result<Unit> =
        apiGateway
            .setUseAnimations(value)
            .onSuccess {
                _useAnimations.value = value
            }

    private val _difficultyAdjustmentFactor: MutableStateFlow<Double> = MutableStateFlow(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR)
    override val difficultyAdjustmentFactor: StateFlow<Double> get() = _difficultyAdjustmentFactor.asStateFlow()

    override suspend fun setDifficultyAdjustmentFactor(value: Double): Result<Unit> {
        // Not applicable for xClients
        return Result.failure(
            UnsupportedOperationException("Difficulty adjustment is not supported on xClients"),
        )
    }

    override suspend fun setNumDaysAfterRedactingTradeData(days: Int): Result<Unit> = apiGateway.setNumDaysAfterRedactingTradeData(days)

    private val _ignoreDiffAdjustmentFromSecManager: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> get() = _ignoreDiffAdjustmentFromSecManager.asStateFlow()

    override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean): Result<Unit> {
        // Not applicable for xClients
        return Result.failure(
            UnsupportedOperationException("Security-manager diff override is not supported on xClients"),
        )
    }

    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    private fun updateLanguage(code: String) {
        if (I18nSupport.currentLanguage != code || _languageCode.value != code) {
            I18nSupport.setLanguage(code)
            _languageCode.value = code
        }
    }

    // API
    override suspend fun getSettings(): Result<SettingsVO> {
        val result = apiGateway.getSettings()
        if (result.isSuccess) {
            result.getOrThrow().let { settings ->
                _tradeRulesConfirmed.value = settings.tradeRulesConfirmed
                updateLanguage(settings.languageCode)
                _useAnimations.value = settings.useAnimations
                return Result.success(settings)
            }
        }
        return result
    }

    override suspend fun getTrustedNodeVersion(): String {
        val trustedNodeApiVersion = apiGateway.getApiVersion().getOrThrow().version
        // return "0.1.1.1" // (for debug)
        return trustedNodeApiVersion
    }
}
