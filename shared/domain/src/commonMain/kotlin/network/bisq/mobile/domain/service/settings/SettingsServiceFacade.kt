package network.bisq.mobile.domain.service.settings

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO

const val DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR = 1.0

interface SettingsServiceFacade : LifeCycleAware {
    suspend fun getSettings(): Result<SettingsVO>

    suspend fun confirmTacAccepted(value: Boolean): Result<Unit>

    val tradeRulesConfirmed: StateFlow<Boolean>

    suspend fun confirmTradeRules(value: Boolean): Result<Unit>

    val languageCode: StateFlow<String>

    suspend fun setLanguageCode(value: String): Result<Unit>

    suspend fun setSupportedLanguageCodes(value: Set<String>): Result<Unit>

    suspend fun setCloseMyOfferWhenTaken(value: Boolean): Result<Unit>

    suspend fun setMaxTradePriceDeviation(value: Double): Result<Unit>

    val useAnimations: StateFlow<Boolean>

    suspend fun setUseAnimations(value: Boolean): Result<Unit>

    val difficultyAdjustmentFactor: StateFlow<Double>

    suspend fun setDifficultyAdjustmentFactor(value: Double): Result<Unit>

    val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean>

    suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean): Result<Unit>

    suspend fun setNumDaysAfterRedactingTradeData(days: Int): Result<Unit>

    suspend fun getTrustedNodeVersion() = ""
}
