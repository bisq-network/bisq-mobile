package network.bisq.mobile.node.common.domain.service.settings

import bisq.common.locale.LocaleRepository
import bisq.common.observable.Pin
import bisq.settings.SettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import java.util.Locale

class NodeSettingsServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    SettingsServiceFacade,
    Logging {
    companion object {
        private fun normalizeLanguageCode(languageCode: String): String {
            if (languageCode.isBlank()) {
                return "en"
            }

            return when {
                // Handle underscore variants (e.g., "pt_BR" -> "pt-BR", "af_ZA" -> "af-ZA")
                languageCode.contains("_") -> languageCode.replace("_", "-")
                // Handle legacy "pcm" -> "pcm-NG"
                languageCode == "pcm" -> "pcm-NG"
                // Handle legacy "en_US" or similar -> just "en"
                languageCode.startsWith("en") && languageCode.length > 2 -> "en"
                else -> languageCode
            }.let { normalized ->
                // Verify the normalized code is supported, otherwise fall back to "en"
                if (I18nSupport.LANGUAGE_CODE_TO_BUNDLE_MAP.containsKey(normalized)) {
                    normalized
                } else {
                    "en"
                }
            }
        }

        private fun languageCodeToLocale(languageCode: String): Locale {
            val normalizedCode = normalizeLanguageCode(languageCode)

            return when (normalizedCode) {
                "af-ZA" -> Locale("af", "ZA")
                "cs" -> Locale("cs", "CZ")
                "de" -> Locale("de", "DE")
                "en" -> Locale("en", "US")
                "es" -> Locale("es", "ES")
                "fr" -> Locale("fr", "FR")
                "hi" -> Locale("hi", "IN")
                "id" -> Locale("id", "ID")
                "it" -> Locale("it", "IT")
                "pcm-NG" -> Locale("pcm", "NG")
                "pt-BR" -> Locale("pt", "BR")
                "ru" -> Locale("ru", "RU")
                "tr" -> Locale("tr", "TR")
                "vi" -> Locale("vi", "VN")
                else -> Locale("en", "US")
            }
        }
    }

    // Dependencies
    private val settingsService: SettingsService by lazy { applicationService.settingsService.get() }

    // Properties

    override suspend fun confirmTacAccepted(value: Boolean): Result<Unit> = runCatching { settingsService.setIsTacAccepted(value) }

    private val _tradeRulesConfirmed = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> get() = _tradeRulesConfirmed.asStateFlow()

    override suspend fun confirmTradeRules(value: Boolean): Result<Unit> = runCatching { settingsService.setBisqEasyTradeRulesConfirmed(value) }

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("")
    override val languageCode: StateFlow<String> get() = _languageCode.asStateFlow()

    override suspend fun setLanguageCode(value: String): Result<Unit> =
        runCatching {
            log.i { "Attempting to set language code to: $value" }
            settingsService.setLanguageTag(value)
            updateLanguage(value)
            log.i { "Successfully set language code to: $value (via Bisq2 core)" }
        }

    override suspend fun setSupportedLanguageCodes(value: Set<String>): Result<Unit> = runCatching { settingsService.supportedLanguageTags.setAll(value) }

    override suspend fun setCloseMyOfferWhenTaken(value: Boolean): Result<Unit> = runCatching { settingsService.setCloseMyOfferWhenTaken(value) }

    override suspend fun setMaxTradePriceDeviation(value: Double): Result<Unit> = runCatching { settingsService.setMaxTradePriceDeviation(value) }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> get() = _useAnimations.asStateFlow()

    override suspend fun setUseAnimations(value: Boolean): Result<Unit> =
        runCatching {
            settingsService.setUseAnimations(value)
            _useAnimations.value = value
        }

    private val _difficultyAdjustmentFactor: MutableStateFlow<Double> = MutableStateFlow(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR)
    override val difficultyAdjustmentFactor: StateFlow<Double> get() = _difficultyAdjustmentFactor.asStateFlow()

    override suspend fun setDifficultyAdjustmentFactor(value: Double): Result<Unit> = runCatching { settingsService.setDifficultyAdjustmentFactor(value) }

    override suspend fun setNumDaysAfterRedactingTradeData(days: Int): Result<Unit> = runCatching { settingsService.setNumDaysAfterRedactingTradeData(days) }

    private val _ignoreDiffAdjustmentFromSecManager: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> get() = _ignoreDiffAdjustmentFromSecManager.asStateFlow()

    override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean): Result<Unit> = runCatching { settingsService.setIgnoreDiffAdjustmentFromSecManager(value) }

    // Misc
    private var tradeRulesConfirmedPin: Pin? = null

    override suspend fun activate() {
        super<ServiceFacade>.activate()
        settingsService.languageTag.addObserver { code ->
            _languageCode.value = code
        }
        tradeRulesConfirmedPin =
            settingsService.bisqEasyTradeRulesConfirmed.addObserver { isConfirmed ->
                _tradeRulesConfirmed.value = isConfirmed
            }
        settingsService.useAnimations.addObserver { value ->
            _useAnimations.value = value
        }
        settingsService.difficultyAdjustmentFactor.addObserver { value ->
            _difficultyAdjustmentFactor.value = value
        }
        settingsService.ignoreDiffAdjustmentFromSecManager.addObserver { value ->
            _ignoreDiffAdjustmentFromSecManager.value = value
        }
    }

    override suspend fun deactivate() {
        tradeRulesConfirmedPin?.unbind()

        super<ServiceFacade>.deactivate()
    }

    private fun updateLanguage(code: String) {
        // Normalize the language code to ensure consistency across all systems
        val normalizedCode = Companion.normalizeLanguageCode(code)

        if (I18nSupport.currentLanguage != normalizedCode || _languageCode.value != normalizedCode) {
            val locale = languageCodeToLocale(normalizedCode)
            LocaleRepository.setDefaultLocale(locale)
            I18nSupport.setLanguage(normalizedCode)
            _languageCode.value = normalizedCode
        }
    }

    // API
    override suspend fun getSettings(): Result<SettingsVO> =
        try {
            val settings = Mappings.SettingsMapping.from(settingsService)
            updateLanguage(settings.languageCode)
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
}
