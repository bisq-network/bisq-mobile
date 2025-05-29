package network.bisq.mobile.android.node.service.settings

import bisq.common.locale.LocaleRepository
import bisq.common.observable.Pin
import bisq.settings.ChatNotificationType
import bisq.settings.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import java.util.*

class NodeSettingsServiceFacade(applicationService: AndroidApplicationService.Provider) : ServiceFacade(), SettingsServiceFacade {
    private val languageToLocaleMap = mapOf(
        "af-ZA" to Locale("af", "ZA"),
        "cs" to Locale("cs", "CZ"),
        "de" to Locale("de", "DE"),
        "en" to Locale("en", "US"),
        "es" to Locale("es", "ES"),
        "it" to Locale("it", "IT"),
        "pcm" to Locale("pcm", "NG"),
        "pt-BR" to Locale("pt", "BR"),
        "ru" to Locale("ru", "RU")
    )
    // Dependencies
    private val settingsService: SettingsService by lazy { applicationService.settingsService.get() }


    // Properties
    private val _isTacAccepted: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    override val isTacAccepted: StateFlow<Boolean?> get() = _isTacAccepted
    override suspend fun confirmTacAccepted(value: Boolean) {
        settingsService.setIsTacAccepted(value)
    }

    private val _tradeRulesConfirmed = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed
    override suspend fun confirmTradeRules(value: Boolean) {
        settingsService.setTradeRulesConfirmed(value)
    }

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("")
    override val languageCode: StateFlow<String> get() = _languageCode
    override suspend fun setLanguageCode(value: String) {
        settingsService.setLanguageCode(value)
        val locale = languageToLocaleMap[value] ?: Locale("en", "US")
        LocaleRepository.setDefaultLocale(locale)
        _languageCode.value = value
    }

    private val _supportedLanguageCodes: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val supportedLanguageCodes: StateFlow<Set<String>> get() = _supportedLanguageCodes
    override suspend fun setSupportedLanguageCodes(value: Set<String>) {
        settingsService.supportedLanguageCodes.setAll(value)
    }

    private val _chatNotificationType: MutableStateFlow<ChatChannelNotificationTypeEnum> =
        MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)
    override val chatNotificationType: StateFlow<ChatChannelNotificationTypeEnum> get() = _chatNotificationType
    override suspend fun setChatNotificationType(value: ChatChannelNotificationTypeEnum) {
        settingsService.setChatNotificationType(
            when (value) {
                ChatChannelNotificationTypeEnum.ALL -> ChatNotificationType.ALL
                ChatChannelNotificationTypeEnum.MENTION -> ChatNotificationType.MENTION
                ChatChannelNotificationTypeEnum.OFF -> ChatNotificationType.OFF
                ChatChannelNotificationTypeEnum.GLOBAL_DEFAULT -> ChatNotificationType.ALL
            }
        )
    }

    private val _closeMyOfferWhenTaken = MutableStateFlow(true)
    override val closeMyOfferWhenTaken: StateFlow<Boolean> get() = _closeMyOfferWhenTaken
    override suspend fun setCloseMyOfferWhenTaken(value: Boolean) {
        settingsService.setCloseMyOfferWhenTaken(value)
    }

    private val _maxTradePriceDeviation = MutableStateFlow(5.0)
    override val maxTradePriceDeviation: StateFlow<Double> get() = _maxTradePriceDeviation
    override suspend fun setMaxTradePriceDeviation(value: Double) {
        settingsService.setMaxTradePriceDeviation(value)
    }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> get() = _useAnimations
    override suspend fun setUseAnimations(value: Boolean) {
        settingsService.setUseAnimations(value)
    }

    private val _difficultyAdjustmentFactor: MutableStateFlow<Double> = MutableStateFlow(1.0)
    override val difficultyAdjustmentFactor: StateFlow<Double> get() = _difficultyAdjustmentFactor
    override suspend fun setDifficultyAdjustmentFactor(value: Double) {
        settingsService.setDifficultyAdjustmentFactor(value)
    }

    private val _numDaysAfterRedactingTradeData = MutableStateFlow(90)
    override val numDaysAfterRedactingTradeData: StateFlow<Int> = _numDaysAfterRedactingTradeData
    override suspend fun setNumDaysAfterRedactingTradeData(days: Int) {
        settingsService.setNumDaysAfterRedactingTradeData(days)
    }

    private val _ignoreDiffAdjustmentFromSecManager: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> get() = _ignoreDiffAdjustmentFromSecManager
    override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean) {
        settingsService.setIgnoreDiffAdjustmentFromSecManager(value)
    }

    // Misc
    private var tradeRulesConfirmedPin: Pin? = null

    override fun activate() {
        super<ServiceFacade>.activate()
        settingsService.languageCode.addObserver { code ->
            _languageCode.value = code
        }
        tradeRulesConfirmedPin = settingsService.isTacAccepted.addObserver { isTacAccepted ->
            _isTacAccepted.value = isTacAccepted
        }
        tradeRulesConfirmedPin = settingsService.tradeRulesConfirmed.addObserver { isConfirmed ->
            _tradeRulesConfirmed.value = isConfirmed
        }
        settingsService.closeMyOfferWhenTaken.addObserver { value ->
            _closeMyOfferWhenTaken.value = value
        }
        settingsService.maxTradePriceDeviation.addObserver { value ->
            _maxTradePriceDeviation.value = value
        }
        settingsService.useAnimations.addObserver { value ->
            _useAnimations.value = value
        }
        settingsService.difficultyAdjustmentFactor.addObserver { value ->
            _difficultyAdjustmentFactor.value = value
        }
        settingsService.numDaysAfterRedactingTradeData.addObserver { value ->
            _numDaysAfterRedactingTradeData.value = value
        }
        settingsService.ignoreDiffAdjustmentFromSecManager.addObserver { value ->
            _ignoreDiffAdjustmentFromSecManager.value = value
        }
    }

    override fun deactivate() {
        tradeRulesConfirmedPin?.unbind()

        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun getSettings(): Result<SettingsVO> {
        return try {
            Result.success(Mappings.SettingsMapping.from(settingsService))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}