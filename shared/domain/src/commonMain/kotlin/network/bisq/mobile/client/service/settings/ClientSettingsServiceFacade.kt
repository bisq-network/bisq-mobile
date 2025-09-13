package network.bisq.mobile.client.service.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.SemanticVersion
import network.bisq.mobile.i18n.I18nSupport

class ClientSettingsServiceFacade(private val apiGateway: SettingsApiGateway) : ServiceFacade(), SettingsServiceFacade, Logging {
    // Properties

    private val _isTacAccepted: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    override val isTacAccepted: StateFlow<Boolean?> get() = _isTacAccepted.asStateFlow()
    override suspend fun confirmTacAccepted(value: Boolean) {
        val result = apiGateway.confirmTacAccepted(value)
        if (result.isSuccess) {
            _isTacAccepted.value = value
        }
    }

    private val _tradeRulesConfirmed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed.asStateFlow()
    override suspend fun confirmTradeRules(value: Boolean) {
        val result = apiGateway.confirmTradeRules(value)
        if (result.isSuccess) {
            _tradeRulesConfirmed.value = value
        }
    }

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("")
    override val languageCode: StateFlow<String> get() = _languageCode.asStateFlow()
    override suspend fun setLanguageCode(value: String) {
        try {
            log.i { "Client attempting to set language code to: $value" }

            // For languages that the backend doesn't support (like "pcm"),
            // we handle them locally without calling the API
            if (value == "pcm") {
                log.i { "Client handling PCM language locally (not supported by backend)" }
                _languageCode.value = value
                log.i { "Client successfully set language code to: $value (local handling)" }
            } else {
                // For languages supported by the backend, use the normal API flow
                val result = apiGateway.setLanguageCode(value)
                if (result.isSuccess) {
                    _languageCode.value = value
                    log.i { "Client successfully set language code to: $value (via API)" }
                } else {
                    log.e { "Client API call failed for language code: $value" }
                }
            }

            I18nSupport.setLanguage(value)
        } catch (e: Exception) {
            log.e(e) { "Client failed to set language code to: $value" }
            throw e
        }
    }

    private val _supportedLanguageCodes: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val supportedLanguageCodes: StateFlow<Set<String>> get() = _supportedLanguageCodes.asStateFlow()

    override suspend fun setSupportedLanguageCodes(value: Set<String>) {
        val result = apiGateway.setSupportedLanguageCodes(value)
        if (result.isSuccess) {
            _supportedLanguageCodes.value = value
        }
    }

    private val _chatNotificationType: MutableStateFlow<ChatChannelNotificationTypeEnum> =
        MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)
    override val chatNotificationType: StateFlow<ChatChannelNotificationTypeEnum> get() = _chatNotificationType.asStateFlow()
    override suspend fun setChatNotificationType(value: ChatChannelNotificationTypeEnum) {
        // Persist remotely removed; keep local state consistent for observers
        _chatNotificationType.value = value
    }

    private val _closeMyOfferWhenTaken = MutableStateFlow(true)
    override val closeMyOfferWhenTaken: StateFlow<Boolean> get() = _closeMyOfferWhenTaken.asStateFlow()
    override suspend fun setCloseMyOfferWhenTaken(value: Boolean) {
        val result = apiGateway.setCloseMyOfferWhenTaken(value)
        if (result.isSuccess) {
            _closeMyOfferWhenTaken.value = value
        }
    }

    private val _maxTradePriceDeviation = MutableStateFlow(5.0)
    override val maxTradePriceDeviation: StateFlow<Double> get() = _maxTradePriceDeviation.asStateFlow()
    override suspend fun setMaxTradePriceDeviation(value: Double) {
        val result = apiGateway.setMaxTradePriceDeviation(value)
        if (result.isSuccess) {
            _maxTradePriceDeviation.value = value
        }
    }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> get() = _useAnimations.asStateFlow()
    override suspend fun setUseAnimations(value: Boolean) {
        val result = apiGateway.setUseAnimations(value)
        if (result.isSuccess) {
            _useAnimations.value = value
        }
    }

    private val _difficultyAdjustmentFactor: MutableStateFlow<Double> = MutableStateFlow(1.0)
    override val difficultyAdjustmentFactor: StateFlow<Double> get() = _difficultyAdjustmentFactor.asStateFlow()
    override suspend fun setDifficultyAdjustmentFactor(value: Double) {
        // Not applicable for xClients
    }

    private val _numDaysAfterRedactingTradeData: MutableStateFlow<Int> = MutableStateFlow(90)
    override val numDaysAfterRedactingTradeData: StateFlow<Int> get() = _numDaysAfterRedactingTradeData.asStateFlow()
    override suspend fun setNumDaysAfterRedactingTradeData(days: Int) {
        val result = apiGateway.setNumDaysAfterRedactingTradeData(days)
        if (result.isSuccess) {
            _numDaysAfterRedactingTradeData.value = days
        }
    }

    private val _ignoreDiffAdjustmentFromSecManager: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> get() = _ignoreDiffAdjustmentFromSecManager.asStateFlow()
    override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean) {
        // Not applicable for xClients
    }

    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getSettings(): Result<SettingsVO> {
        val result = apiGateway.getSettings()
        if (result.isSuccess) {
            result.getOrThrow().let { settings ->
                _isTacAccepted.value = settings.isTacAccepted
                _tradeRulesConfirmed.value = settings.tradeRulesConfirmed

                // Only update language code from API if we're not using a locally-handled language
                if (_languageCode.value != "pcm") {
                    _languageCode.value = settings.languageCode
                }

                _maxTradePriceDeviation.value = settings.maxTradePriceDeviation
                _supportedLanguageCodes.value = settings.supportedLanguageCodes
                _closeMyOfferWhenTaken.value = settings.closeMyOfferWhenTaken
                _useAnimations.value = settings.useAnimations
                _numDaysAfterRedactingTradeData.value = settings.numDaysAfterRedactingTradeData

                // If we're using a locally-handled language, override the returned settings
                val actualLanguageCode = if (_languageCode.value == "pcm") "pcm" else settings.languageCode
                val correctedSettings = settings.copy(languageCode = actualLanguageCode)
                return Result.success(correctedSettings)
            }
        }
        return result
    }

    override suspend fun isApiCompatible(): Boolean {
        val requiredVersion = BuildConfig.BISQ_API_VERSION
        val result = apiGateway.getApiVersion()
        if (result.isSuccess) {
            val nodeApiVersion = apiGateway.getApiVersion().getOrThrow().version
            log.d { "required trusted node api version is $requiredVersion and current is $nodeApiVersion" }
            return SemanticVersion.from(nodeApiVersion) >= SemanticVersion.from(requiredVersion)
        } else {
            log.w { "Could not read trusted node API version; assuming incompatible." }
            return false
        }
    }

    override suspend fun getTrustedNodeVersion(): String {
        val trustedNodeApiVersion = apiGateway.getApiVersion().getOrThrow().version
        // return "0.1.1.1" // (for debug)
        return trustedNodeApiVersion
    }

}