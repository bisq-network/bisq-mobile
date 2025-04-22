package network.bisq.mobile.client.service.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientSettingsServiceFacade(private val apiGateway: SettingsApiGateway) : SettingsServiceFacade, Logging {
    // Properties

    private val _isTacAccepted: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    override val isTacAccepted: StateFlow<Boolean?> get() = _isTacAccepted
    override suspend fun confirmTacAccepted(value: Boolean) {
        val result = apiGateway.confirmTacAccepted(value)
        if (result.isSuccess) {
            _isTacAccepted.value = value
        }
    }

    private val _tradeRulesConfirmed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed
    override suspend fun confirmTradeRules(value: Boolean) {
        val result = apiGateway.confirmTradeRules(value)
        if (result.isSuccess) {
            _tradeRulesConfirmed.value = value
        }
    }

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("")
    override val languageCode: StateFlow<String> get() = _languageCode
    override suspend fun setLanguageCode(value: String) {
        val result = apiGateway.setLanguageCode(value)
        if (result.isSuccess) {
            _languageCode.value = value
        }
    }

    private val _supportedLanguageCodes: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val supportedLanguageCodes: StateFlow<Set<String>> get() = _supportedLanguageCodes

    override suspend fun setSupportedLanguageCodes(value: Set<String>) {
        val result = apiGateway.setSupportedLanguageCodes(value)
        if (result.isSuccess) {
            _supportedLanguageCodes.value = value
        }
    }

    private val _chatNotificationType: MutableStateFlow<ChatChannelNotificationTypeEnum> =
        MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)
    override val chatNotificationType: StateFlow<ChatChannelNotificationTypeEnum> get() = _chatNotificationType
    override suspend fun setChatNotificationType(value: ChatChannelNotificationTypeEnum) {
        TODO()
    }

    private val _closeMyOfferWhenTaken = MutableStateFlow(true)
    override val closeMyOfferWhenTaken: StateFlow<Boolean> get() = _closeMyOfferWhenTaken
    override suspend fun setCloseMyOfferWhenTaken(value: Boolean) {
        val result = apiGateway.setCloseMyOfferWhenTaken(value)
        if (result.isSuccess) {
            _closeMyOfferWhenTaken.value = value
        }
    }

    private val _maxTradePriceDeviation = MutableStateFlow(5.0)
    override val maxTradePriceDeviation: StateFlow<Double> get() = _maxTradePriceDeviation
    override suspend fun setMaxTradePriceDeviation(value: Double) {
        val result = apiGateway.setMaxTradePriceDeviation(value)
        if (result.isSuccess) {
            _maxTradePriceDeviation.value = value
        }
    }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> get() = _useAnimations
    override suspend fun setUseAnimations(value: Boolean) {
        val result = apiGateway.setUseAnimations(value)
        if (result.isSuccess) {
            _useAnimations.value = value
        }
    }

    private val _difficultyAdjustmentFactor: MutableStateFlow<Double> = MutableStateFlow(1.0)
    override val difficultyAdjustmentFactor: StateFlow<Double> get() = _difficultyAdjustmentFactor
    override suspend fun setDifficultyAdjustmentFactor(value: Double) {
        // Not applicable for xClients
    }

    private val _numDaysAfterRedactingTradeData: MutableStateFlow<Int> = MutableStateFlow(90)
    override val numDaysAfterRedactingTradeData: StateFlow<Int> get() = _numDaysAfterRedactingTradeData
    override suspend fun setNumDaysAfterRedactingTradeData(days: Int) {
        val result = apiGateway.setNumDaysAfterRedactingTradeData(days)
        if (result.isSuccess) {
            _numDaysAfterRedactingTradeData.value = days
        }
    }

    private val _ignoreDiffAdjustmentFromSecManager: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> get() = _ignoreDiffAdjustmentFromSecManager
    override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean) {
        // Not applicable for xClients
    }

    // API
    override suspend fun getSettings(): Result<SettingsVO> {
        val result = apiGateway.getSettings()
        if (result.isSuccess) {
            result.getOrThrow().let {
                _isTacAccepted.value = it.isTacAccepted
                _tradeRulesConfirmed.value = it.tradeRulesConfirmed
                _languageCode.value = it.languageCode
                _maxTradePriceDeviation.value = it.maxTradePriceDeviation
                _supportedLanguageCodes.value = it.supportedLanguageCodes
                _closeMyOfferWhenTaken.value = it.closeMyOfferWhenTaken
                _useAnimations.value = it.useAnimations
                _numDaysAfterRedactingTradeData.value = it.numDaysAfterRedactingTradeData
            }
        }
        return result
    }

    override suspend fun isApiCompatible(): Boolean {
        val appApiRequiredVersion = BuildConfig.BISQ_API_VERSION
        val trustedNodeApiVersion = apiGateway.getApiVersion().getOrThrow().version
        log.d { "required trusted node api version is $appApiRequiredVersion and current is $trustedNodeApiVersion" }
        // return false // (for debug)
        return trustedNodeApiVersion >= appApiRequiredVersion
    }

    override suspend fun getTrustedNodeVersion(): String {
        val trustedNodeApiVersion = apiGateway.getApiVersion().getOrThrow().version
        // return "0.1.1.1" // (for debug)
        return trustedNodeApiVersion
    }

}