package network.bisq.mobile.domain.service.settings

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO

interface SettingsServiceFacade : LifeCycleAware {

    suspend fun getSettings(): Result<SettingsVO>

    val isTacAccepted: StateFlow<Boolean?>
    suspend fun confirmTacAccepted(value: Boolean)

    val tradeRulesConfirmed: StateFlow<Boolean>
    suspend fun confirmTradeRules(value: Boolean)

    val languageCode: StateFlow<String>
    suspend fun setLanguageCode(value: String)

    val supportedLanguageCodes: StateFlow<Set<String>>
    suspend fun setSupportedLanguageCodes(value: Set<String>)

    val chatNotificationType: StateFlow<ChatChannelNotificationTypeEnum>
    suspend fun setChatNotificationType(value: ChatChannelNotificationTypeEnum)

    val closeMyOfferWhenTaken: StateFlow<Boolean>
    suspend fun setCloseMyOfferWhenTaken(value: Boolean)

    val maxTradePriceDeviation: StateFlow<Double>
    suspend fun setMaxTradePriceDeviation(value: Double)

    val useAnimations: StateFlow<Boolean>
    suspend fun setUseAnimations(value: Boolean)

    val difficultyAdjustmentFactor: StateFlow<Double>
    suspend fun setDifficultyAdjustmentFactor(value: Double)

    val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean>
    suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean)

    val numDaysAfterRedactingTradeData: StateFlow<Int>
    suspend fun setNumDaysAfterRedactingTradeData(days: Int)

    suspend fun isApiCompatible() = true
}