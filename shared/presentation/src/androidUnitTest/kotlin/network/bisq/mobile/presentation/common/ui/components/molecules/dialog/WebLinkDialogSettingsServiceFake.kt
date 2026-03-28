package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.data.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.data.service.settings.SettingsServiceFacade

/**
 * In-memory [SettingsServiceFacade] for [WebLinkConfirmationDialog] tests: updates
 * [showWebLinkConfirmation] and [permitOpeningBrowser] like production when the user persists choices.
 */
internal class WebLinkDialogSettingsServiceFake(
    initialShowWebLinkConfirmation: Boolean = true,
    initialPermitOpeningBrowser: Boolean = true,
) : SettingsServiceFacade {
    private val _showWebLinkConfirmation = MutableStateFlow(initialShowWebLinkConfirmation)
    override val showWebLinkConfirmation: StateFlow<Boolean> = _showWebLinkConfirmation.asStateFlow()

    private val _permitOpeningBrowser = MutableStateFlow(initialPermitOpeningBrowser)
    override val permitOpeningBrowser: StateFlow<Boolean> = _permitOpeningBrowser.asStateFlow()

    private val _tradeRulesConfirmed = MutableStateFlow(false)
    override val tradeRulesConfirmed: StateFlow<Boolean> = _tradeRulesConfirmed.asStateFlow()

    private val _languageCode = MutableStateFlow("en")
    override val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _useAnimations = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> = _useAnimations.asStateFlow()

    private val _difficultyAdjustmentFactor = MutableStateFlow(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR)
    override val difficultyAdjustmentFactor: StateFlow<Double> = _difficultyAdjustmentFactor.asStateFlow()

    private val _ignoreDiffAdjustmentFromSecManager = MutableStateFlow(false)
    override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> =
        _ignoreDiffAdjustmentFromSecManager.asStateFlow()

    fun snapshotShowWebLinkConfirmation(): Boolean = _showWebLinkConfirmation.value

    fun snapshotPermitOpeningBrowser(): Boolean = _permitOpeningBrowser.value

    override suspend fun setPermitOpeningBrowser(value: Boolean): Result<Unit> {
        _permitOpeningBrowser.value = value
        return Result.success(Unit)
    }

    override suspend fun setWebLinkDontShowAgain(): Result<Unit> {
        _showWebLinkConfirmation.value = false
        return Result.success(Unit)
    }

    override suspend fun getSettings(): Result<SettingsVO> = Result.success(settingsVODemoObj)

    override suspend fun confirmTacAccepted(value: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun confirmTradeRules(value: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun setLanguageCode(value: String): Result<Unit> {
        _languageCode.value = value
        return Result.success(Unit)
    }

    override suspend fun setSupportedLanguageCodes(value: Set<String>): Result<Unit> = Result.success(Unit)

    override suspend fun setCloseMyOfferWhenTaken(value: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun setMaxTradePriceDeviation(value: Double): Result<Unit> = Result.success(Unit)

    override suspend fun setUseAnimations(value: Boolean): Result<Unit> {
        _useAnimations.value = value
        return Result.success(Unit)
    }

    override suspend fun setDifficultyAdjustmentFactor(value: Double): Result<Unit> {
        _difficultyAdjustmentFactor.value = value
        return Result.success(Unit)
    }

    override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean): Result<Unit> {
        _ignoreDiffAdjustmentFromSecManager.value = value
        return Result.success(Unit)
    }

    override suspend fun setNumDaysAfterRedactingTradeData(days: Int): Result<Unit> = Result.success(Unit)

    override suspend fun resetAllDontShowAgainFlags(): Result<Unit> = Result.success(Unit)

    override suspend fun getTrustedNodeVersion(): String = ""
}
