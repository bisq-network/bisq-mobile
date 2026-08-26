package network.bisq.mobile.data.service.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.data.service.LifeCycleAware

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

    val showWebLinkConfirmation: StateFlow<Boolean>

    suspend fun setWebLinkDontShowAgain(): Result<Unit>

    suspend fun resetAllDontShowAgainFlags(): Result<Unit>

    val permitOpeningBrowser: StateFlow<Boolean>

    suspend fun setPermitOpeningBrowser(value: Boolean): Result<Unit>

    suspend fun getTrustedNodeVersion() = ""

    /**
     * Whether this backend can read/write the bisq2 core "auto-add trade peers to contacts"
     * True on the node (core runs in-process); false on the client until the
     * trusted-node API exposes it — the Settings row hides itself on false.
     */
    val isAutoAddTradePeersToContactsSupported: Boolean get() = false

    /** bisq2 core `autoAddToContactsList` — default ON upstream. */
    val autoAddTradePeersToContacts: StateFlow<Boolean> get() = UnsupportedAutoAddTradePeersToContacts

    suspend fun setAutoAddTradePeersToContacts(value: Boolean): Result<Unit> = Result.failure(UnsupportedOperationException("auto-add to contacts is not supported by this backend"))
}

// Shared read-only default for backends (and test fakes) that don't support the setting.
private val UnsupportedAutoAddTradePeersToContacts = MutableStateFlow(true)
