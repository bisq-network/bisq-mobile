package network.bisq.mobile.client.common.domain.service.settings

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.settings.CookieKey
import network.bisq.mobile.data.replicated.settings.DontShowAgainKey
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.I18nSupport
import kotlin.coroutines.cancellation.CancellationException

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

    private val _showWebLinkConfirmation: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val showWebLinkConfirmation: StateFlow<Boolean> get() = _showWebLinkConfirmation.asStateFlow()

    override suspend fun setWebLinkDontShowAgain(): Result<Unit> {
        val result = apiGateway.setWebLinkDontShowAgain()
        _showWebLinkConfirmation.value = !result.isSuccess
        return result
    }

    override suspend fun resetAllDontShowAgainFlags(): Result<Unit> {
        val result = apiGateway.resetAllDontShowAgainFlags()
        _showWebLinkConfirmation.value = result.isSuccess
        return result
    }

    private val _permitOpeningBrowser: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val permitOpeningBrowser: StateFlow<Boolean> get() = _permitOpeningBrowser.asStateFlow()

    override suspend fun setPermitOpeningBrowser(value: Boolean): Result<Unit> {
        var result: Result<Unit>
        if (value) {
            result = apiGateway.setCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal)
        } else {
            result = apiGateway.unsetCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal)
        }
        if (result.isSuccess) {
            _permitOpeningBrowser.value = value
        }
        return result
    }

    override suspend fun activate() {
        super<ServiceFacade>.activate()
        fetchOpeningPermission()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    private var cookieJob: Job? = null

    fun fetchOpeningPermission() {
        cookieJob?.cancel()
        cookieJob =
            jobsManager.getScope().launch {
                val result = getCookie(CookieKey.PERMIT_OPENING_BROWSER.ordinal)

                result.exceptionOrNull()?.let { error ->
                    if (error is CancellationException) throw error
                    return@launch
                }
                val value = result.getOrNull() ?: false
                _permitOpeningBrowser.value = value
            }
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
                _showWebLinkConfirmation.value =
                    (settings.dontShowAgainMap[DontShowAgainKey.HYPERLINKS_OPEN_IN_BROWSER.getKey()] ?: false) == false
                return Result.success(settings)
            }
        }
        return result
    }

    suspend fun getCookie(key: Int): Result<Boolean> {
        val result = apiGateway.getCookie(key)
        if (result.isSuccess) {
            result.getOrThrow().let { value ->
                return Result.success(value)
            }
        }
        return result
    }

    suspend fun setCookie(
        key: Int,
        value: Boolean,
    ): Result<Unit> {
        var result: Result<Unit>
        if (value) {
            result = apiGateway.setCookie(key)
        } else {
            result = apiGateway.unsetCookie(key)
        }
        if (result.isSuccess) {
            result.getOrThrow().let { newValue ->
                return Result.success(Unit)
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
