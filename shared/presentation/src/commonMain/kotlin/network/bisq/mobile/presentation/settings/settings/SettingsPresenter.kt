package network.bisq.mobile.presentation.settings.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.settings.DEFAULT_MAX_TRADE_PRICE_DEVIATION
import network.bisq.mobile.data.replicated.settings.DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA
import network.bisq.mobile.data.service.common.LanguageServiceFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.data.utils.setDefaultLocale
import network.bisq.mobile.data.utils.toDoubleOrNullLocaleAware
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.i18n.DEFAULT_LANGUAGE_CODE
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.base.SnackbarPosition
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.main.MainPresenter

open class SettingsPresenter(
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val pushNotificationServiceFacade: PushNotificationServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val animationSettings: AnimationSettings,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.Settings

    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                tradePriceTolerance = createTradePriceToleranceDataEntry(NumberFormatter.format(DEFAULT_MAX_TRADE_PRICE_DEVIATION * 100)),
                numDaysAfterRedactingTradeData = createNumDaysAfterRedactingTradeDataDataEntry(DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA.toString()),
                powFactor = createPowFactorDataEntry(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR.toString()),
            ),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _isTradePriceToleranceSaveEnabled = MutableStateFlow(true)
    val isTradePriceToleranceSaveEnabled: StateFlow<Boolean> = _isTradePriceToleranceSaveEnabled.asStateFlow()

    private val _isNumDaysAfterRedactingTradeDataSaveEnabled = MutableStateFlow(true)
    val isNumDaysAfterRedactingTradeDataSaveEnabled: StateFlow<Boolean> =
        _isNumDaysAfterRedactingTradeDataSaveEnabled.asStateFlow()

    private val _isPowFactorSaveEnabled = MutableStateFlow(true)
    val isPowFactorSaveEnabled: StateFlow<Boolean> = _isPowFactorSaveEnabled.asStateFlow()

    private val _isPushNotificationsToggleEnabled = MutableStateFlow(true)
    val isPushNotificationsToggleEnabled: StateFlow<Boolean> = _isPushNotificationsToggleEnabled.asStateFlow()

    private val _isLanguageCodeChangeEnabled = MutableStateFlow(true)
    val isLanguageCodeChangeEnabled: StateFlow<Boolean> = _isLanguageCodeChangeEnabled.asStateFlow()

    private val _isSupportedLanguageCodesChangeEnabled = MutableStateFlow(true)
    val isSupportedLanguageCodesChangeEnabled: StateFlow<Boolean> =
        _isSupportedLanguageCodesChangeEnabled.asStateFlow()

    private val _isCloseOfferWhenTradeTakenChangeEnabled = MutableStateFlow(true)
    val isCloseOfferWhenTradeTakenChangeEnabled: StateFlow<Boolean> =
        _isCloseOfferWhenTradeTakenChangeEnabled.asStateFlow()

    // Starts disabled (greyed) when the device is low-spec: animations are force-off
    // and the toggle can't be turned on. This reuses the existing in-flight guard flag — on a locked
    // device setUseAnimations is never invoked, so nothing re-enables it.
    private val _isUseAnimationsChangeEnabled = MutableStateFlow(!animationSettings.lockedByDevice)
    val isUseAnimationsChangeEnabled: StateFlow<Boolean> = _isUseAnimationsChangeEnabled.asStateFlow()

    private val _isIgnorePowChangeEnabled = MutableStateFlow(true)
    val isIgnorePowChangeEnabled: StateFlow<Boolean> = _isIgnorePowChangeEnabled.asStateFlow()

    private val _isResetAllDontShowAgainEnabled = MutableStateFlow(true)
    val isResetAllDontShowAgainEnabled: StateFlow<Boolean> = _isResetAllDontShowAgainEnabled.asStateFlow()

    open val shouldShowPoWAdjustmentFactor = false

    /**
     * Whether the relayed-push-notifications opt-in toggle should be shown.
     * - Android Connect: true (FCM-relayed path is wired through the trusted node).
     * - iOS Connect: false (APNs-relayed path is not yet wired end-to-end; exposing
     *   the toggle would let users opt in to a delivery path that doesn't work).
     * - Node (Android only): overridden to false because the embedded Bisq2 process
     *   posts notifications through its local foreground service.
     */
    open val shouldShowPushNotificationsToggle: Boolean
        get() = getPlatformInfo().type == PlatformType.ANDROID

    /**
     * Whether the "keep connected in background" sub-setting should be shown.
     *
     * Android-only by design. The setting controls the local foreground service —
     * Android's only mechanism for keeping the WebSocket alive in background. iOS
     * has no equivalent (research concluded that NSURLSessionWebSocketTask + APNs
     * is the practical ceiling), so the toggle is suppressed there even though
     * the parent push-notifications toggle could in principle apply.
     *
     * Visibility is further gated in the UI by [SettingsUiState.pushNotificationsEnabled] —
     * keep-connected is a sub-option of relayed and only appears when relayed is on.
     */
    open val shouldShowKeepConnectedToggle: Boolean
        get() = getPlatformInfo().type == PlatformType.ANDROID

    // Store original values from fetchSettings for cancel operations (raw numeric values)
    private var originalMaxTradePriceDeviation: Double = DEFAULT_MAX_TRADE_PRICE_DEVIATION
    private var originalNumDaysAfterRedactingTradeData: Int = DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA
    private var originalDifficultyAdjustmentFactor: Double = DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR

    override fun onViewAttached() {
        super.onViewAttached()
        fetchSettings()
        observePushNotificationsEnabled()
        observeKeepConnectedInBackground()
        observeAnalyticsEnabled()
        observeRememberOfferbookFilterPreferences()
    }

    private fun observePushNotificationsEnabled() {
        // Reflect the facade's StateFlow into our UI state. The facade itself
        // stays the source of truth — re-registers, server roundtrips, and
        // permission revocations from OS Settings all flow through here.
        presenterScope.launch {
            pushNotificationServiceFacade.isPushNotificationsEnabled.collect { enabled ->
                _uiState.update {
                    it.copy(
                        pushNotificationsEnabled = enabled,
                        shouldShowPushNotificationsToggle = shouldShowPushNotificationsToggle,
                        shouldShowKeepConnectedToggle = shouldShowKeepConnectedToggle,
                    )
                }
            }
        }
    }

    /**
     * Observe the persisted "keep connected in background" preference. Reflects
     * any change made elsewhere (e.g. cleared via hide-implies-reset when the
     * user turns relayed off) directly into the UI state.
     */
    private fun observeKeepConnectedInBackground() {
        presenterScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update {
                    it.copy(keepConnectedInBackground = settings.keepConnectedInBackground)
                }
            }
        }
    }

    /**
     * Reflect the persisted analytics opt-in state into the UI. Source of truth
     * is `SettingsRepository.analyticsEnabled` — the DI module reads the same
     * value via [SettingsRepository.analyticsEnabledIn] for the SDK's runtime
     * gate, so a flip from here propagates to emission within the next track()
     * call without any extra plumbing.
     */
    private fun observeAnalyticsEnabled() {
        presenterScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update {
                    it.copy(analyticsEnabled = settings.analyticsEnabled)
                }
            }
        }
    }

    private fun observeRememberOfferbookFilterPreferences() {
        presenterScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update {
                    it.copy(rememberOfferbookFilterPreferences = settings.rememberOfferbookFilterPreferences)
                }
            }
        }
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.OnLanguageCodeChange -> setLanguageCode(action.langCode)
            is SettingsUiAction.OnSupportedLanguageCodeToggle -> {
                setSupportedLanguageCodes(action.key, action.selected)
            }
            is SettingsUiAction.OnCloseOfferWhenTradeTakenChange -> setCloseOfferWhenTradeTaken(action.value)
            is SettingsUiAction.OnTradePriceToleranceChange -> onTradePriceToleranceChange(action.value)
            is SettingsUiAction.OnTradePriceToleranceFocus -> onTradePriceToleranceFocus(action.hasFocus)
            SettingsUiAction.OnTradePriceToleranceSave -> onTradePriceToleranceSave()
            SettingsUiAction.OnTradePriceToleranceCancel -> onTradePriceToleranceCancel()
            is SettingsUiAction.OnUseAnimationsChange -> setUseAnimations(action.value)
            SettingsUiAction.OnUseAnimationsLockedTap ->
                showSnackbar("settings.display.useAnimations.lockedByDevice".i18n())
            is SettingsUiAction.OnRememberOfferbookFilterPreferencesChange ->
                setRememberOfferbookFilterPreferences(action.enabled)
            is SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange ->
                onNumDaysAfterRedactingTradeDataChange(action.value)

            is SettingsUiAction.OnNumDaysAfterRedactingTradeDataFocus ->
                onNumDaysAfterRedactingTradeDataFocus(action.hasFocus)

            SettingsUiAction.OnNumDaysAfterRedactingTradeDataSave -> onNumDaysAfterRedactingTradeDataSave()
            SettingsUiAction.OnNumDaysAfterRedactingTradeDataCancel -> onNumDaysAfterRedactingTradeDataCancel()
            is SettingsUiAction.OnPowFactorChange -> onPowFactorChange(action.value)
            is SettingsUiAction.OnPowFactorFocus -> onPowFactorFocus(action.hasFocus)
            SettingsUiAction.OnPowFactorSave -> onPowFactorSave()
            SettingsUiAction.OnPowFactorCancel -> onPowFactorCancel()
            is SettingsUiAction.OnIgnorePowChange -> setIgnorePow(action.value)
            SettingsUiAction.OnResetAllDontShowAgainClick -> onResetAllDontShowAgainClick()
            SettingsUiAction.OnRetryLoadSettingsClick -> fetchSettings()
            is SettingsUiAction.OnPushNotificationsToggle -> onPushNotificationsToggle(action.enabled)
            SettingsUiAction.OnPushNotificationsLearnMore ->
                navigateToUrl(BisqLinks.BISQ_CONNECT_PUSH_NOTIFICATIONS_WIKI_URL)

            is SettingsUiAction.OnKeepConnectedInBackgroundToggle ->
                onKeepConnectedInBackgroundToggle(action.enabled)

            is SettingsUiAction.OnAnalyticsToggle -> onAnalyticsToggle(action.enabled)
            SettingsUiAction.OnAnalyticsLearnMore ->
                navigateToUrl(BisqLinks.BISQ_MOBILE_ANALYTICS_WIKI_URL)
        }
    }

    private fun onAnalyticsToggle(enabled: Boolean) {
        // Track ordering depends on direction:
        //  - DISABLE: track BEFORE persist so the event ships through the SDK
        //    gate while it's still open. Track-after-persist would let the
        //    StateFlow propagation close the gate first and the event would
        //    be dropped by `runtimeOptInProvider`.
        //  - ENABLE: track AFTER persist for the symmetric reason. In the
        //    re-opt-in-after-opt-out case the SDK is already initialised so
        //    `track()` forwards direct; the gate would still read the OLD
        //    `analyticsEnabled=false` if we tracked first, and the event
        //    would be silently dropped. In the first-ever opt-in case the
        //    SDK isn't ready yet so the event gets buffered either way —
        //    ordering doesn't hurt the cold-start path.
        if (!enabled) {
            analyticsService?.track(AnalyticsEvent.Settings.AnalyticsDisabled)
        }
        presenterScope.launch {
            // Persist via the repo. The DI module's hot StateFlow view of
            // analyticsEnabled picks this up on the next emission and the
            // SDK's runtimeOptInProvider reflects the new value on the next
            // track() call. Also mark the prompt as seen so the welcome
            // carousel won't auto-prompt later if the user already engaged.
            //
            // On opt-OUT, also reset `analyticsBaselineSent` to false. The
            // baseline-snapshot mechanism in AnalyticsSettingsBaseline.emit()
            // checks this flag to avoid re-emitting on every cold start;
            // resetting on opt-out guarantees that if the user opts back in
            // later, they get a fresh baseline (their settings may have
            // changed during the opt-out interval). Single atomic write keeps
            // the two flags in sync — no risk of "opted out but still flagged
            // as baselined" leaking into the next opt-in cycle.
            settingsRepository.update {
                it.copy(
                    analyticsEnabled = enabled,
                    analyticsPromptSeen = true,
                    analyticsBaselineSent = if (enabled) it.analyticsBaselineSent else false,
                )
            }
            if (enabled) {
                analyticsService?.track(AnalyticsEvent.Settings.AnalyticsEnabled)
            }
        }
    }

    private fun onKeepConnectedInBackgroundToggle(enabled: Boolean) {
        presenterScope.launch {
            settingsRepository.update { it.copy(keepConnectedInBackground = enabled) }
        }
        val event =
            if (enabled) {
                AnalyticsEvent.Settings.KeepConnectedEnabled
            } else {
                AnalyticsEvent.Settings.KeepConnectedDisabled
            }
        analyticsService?.track(event)
    }

    private fun onPushNotificationsToggle(enabled: Boolean) {
        guardedSuspendAction(_isPushNotificationsToggleEnabled, "onPushNotificationsToggle") {
            val result =
                if (enabled) {
                    pushNotificationServiceFacade.registerForPushNotifications()
                } else {
                    pushNotificationServiceFacade.unregisterFromPushNotifications()
                }
            result
                .onSuccess {
                    // Hide-implies-reset: when relayed is turned off the keep-connected sub-toggle
                    // disappears from the UI. Persist the reset so re-enabling relayed later
                    // starts fresh on the default (OFF) rather than remembering a previously
                    // chosen power-user combo the user can no longer see or undo.
                    if (!enabled) {
                        settingsRepository.update { it.copy(keepConnectedInBackground = false) }
                    }
                    val msgKey =
                        if (enabled) {
                            "mobile.pushNotifications.registrationSuccess"
                        } else {
                            "mobile.pushNotifications.toggleOffSuccess"
                        }
                    showSnackbar(msgKey.i18n())
                    // Track only on success — a failed register/unregister means
                    // the toggle didn't actually take effect, so emitting would
                    // misrepresent state. handleError path below emits nothing.
                    val event =
                        if (enabled) {
                            AnalyticsEvent.Settings.PushNotificationsEnabled
                        } else {
                            AnalyticsEvent.Settings.PushNotificationsDisabled
                        }
                    analyticsService?.track(event)
                }.onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    private fun setLanguageCode(langCode: String) {
        guardedSuspendAction(_isLanguageCodeChangeEnabled, "setLanguageCode") {
            settingsServiceFacade
                .setLanguageCode(langCode)
                .onSuccess {
                    try {
                        setDefaultLocale(langCode)
                        _uiState.update { it.copy(languageCode = langCode) }
                    } catch (e: Exception) {
                        showSnackbar(e.message ?: "mobile.error.generic".i18n(), SnackbarType.ERROR)
                    }
                }.onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    private fun setSupportedLanguageCodes(
        langCode: String,
        selected: Boolean,
    ) {
        guardedSuspendAction(_isSupportedLanguageCodesChangeEnabled, "setSupportedLanguageCodes") {
            val current = _uiState.value.supportedLanguageCodes
            val next = if (selected) current + langCode else current - langCode
            _uiState.update { it.copy(supportedLanguageCodes = next) }

            settingsServiceFacade
                .setSupportedLanguageCodes(next)
                .onFailure { exception ->
                    _uiState.update { it.copy(supportedLanguageCodes = current) }
                    handleError(exception, position = SnackbarPosition.TOP)
                }
        }
    }

    private fun setCloseOfferWhenTradeTaken(value: Boolean) {
        guardedSuspendAction(_isCloseOfferWhenTradeTakenChangeEnabled, "setCloseOfferWhenTradeTaken") {
            _uiState.update { it.copy(closeOfferWhenTradeTaken = value) }
            settingsServiceFacade
                .setCloseMyOfferWhenTaken(value)
                .onFailure { exception ->
                    _uiState.update { it.copy(closeOfferWhenTradeTaken = !value) }
                    handleError(exception)
                }
        }
    }

    private fun onTradePriceToleranceChange(value: String) {
        _uiState.update {
            val newEntry = it.tradePriceTolerance.updateValue(value)
            val parsedValue = newEntry.value.toDoubleOrNullLocaleAware()
            val hasChanges = parsedValue != null && parsedValue != originalMaxTradePriceDeviation * 100
            it.copy(
                tradePriceTolerance = newEntry,
                hasChangesTradePriceTolerance = hasChanges,
            )
        }
    }

    private fun onTradePriceToleranceFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            _uiState.update {
                it.copy(tradePriceTolerance = it.tradePriceTolerance.validate())
            }
        }
    }

    private fun onTradePriceToleranceSave() {
        _uiState.update {
            it.copy(tradePriceTolerance = it.tradePriceTolerance.validate())
        }
        val currentEntry = _uiState.value.tradePriceTolerance
        if (!currentEntry.isValid) return
        val parsedValue = currentEntry.value.toDoubleOrNullLocaleAware() ?: return
        val newDeviation = parsedValue / 100

        guardedSuspendAction(_isTradePriceToleranceSaveEnabled, "onTradePriceToleranceSave") {
            settingsServiceFacade
                .setMaxTradePriceDeviation(newDeviation)
                .onSuccess {
                    originalMaxTradePriceDeviation = newDeviation
                    _uiState.update {
                        it.copy(hasChangesTradePriceTolerance = false)
                    }
                }.onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    private fun onTradePriceToleranceCancel() {
        val formattedValue = NumberFormatter.format(originalMaxTradePriceDeviation * 100)
        _uiState.update {
            it.copy(
                tradePriceTolerance = it.tradePriceTolerance.updateValue(formattedValue),
                hasChangesTradePriceTolerance = false,
            )
        }
    }

    private fun onNumDaysAfterRedactingTradeDataChange(value: String) {
        _uiState.update {
            val newEntry = it.numDaysAfterRedactingTradeData.updateValue(value)
            val parsedValue = newEntry.value.toIntOrNull()
            val hasChanges = parsedValue != null && parsedValue != originalNumDaysAfterRedactingTradeData
            it.copy(
                numDaysAfterRedactingTradeData = newEntry,
                hasChangesNumDaysAfterRedactingTradeData = hasChanges,
            )
        }
    }

    private fun onNumDaysAfterRedactingTradeDataFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            _uiState.update {
                it.copy(numDaysAfterRedactingTradeData = it.numDaysAfterRedactingTradeData.validate())
            }
        }
    }

    private fun onNumDaysAfterRedactingTradeDataSave() {
        _uiState.update {
            it.copy(numDaysAfterRedactingTradeData = it.numDaysAfterRedactingTradeData.validate())
        }
        val currentEntry = _uiState.value.numDaysAfterRedactingTradeData
        if (!currentEntry.isValid) return
        val parsedValue = currentEntry.value.toIntOrNull() ?: return

        guardedSuspendAction(_isNumDaysAfterRedactingTradeDataSaveEnabled, "onNumDaysAfterRedactingTradeDataSave") {
            settingsServiceFacade
                .setNumDaysAfterRedactingTradeData(parsedValue)
                .onSuccess {
                    originalNumDaysAfterRedactingTradeData = parsedValue
                    _uiState.update {
                        it.copy(hasChangesNumDaysAfterRedactingTradeData = false)
                    }
                }.onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    private fun onNumDaysAfterRedactingTradeDataCancel() {
        _uiState.update {
            it.copy(
                numDaysAfterRedactingTradeData =
                    it.numDaysAfterRedactingTradeData.updateValue(
                        originalNumDaysAfterRedactingTradeData.toString(),
                    ),
                hasChangesNumDaysAfterRedactingTradeData = false,
            )
        }
    }

    private fun setUseAnimations(value: Boolean) {
        guardedSuspendAction(_isUseAnimationsChangeEnabled, "setUseAnimations") {
            _uiState.update { it.copy(useAnimations = value) }
            settingsServiceFacade
                .setUseAnimations(value)
                .onFailure { exception ->
                    _uiState.update { it.copy(useAnimations = !value) }
                    handleError(exception)
                }
        }
    }

    private fun setRememberOfferbookFilterPreferences(enabled: Boolean) {
        presenterScope.launch {
            val previous = _uiState.value.rememberOfferbookFilterPreferences
            _uiState.update { it.copy(rememberOfferbookFilterPreferences = enabled) }
            try {
                settingsRepository.setRememberOfferbookFilterPreferences(enabled)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(rememberOfferbookFilterPreferences = previous) }
                handleError(exception)
            }
        }
    }

    private fun onPowFactorChange(value: String) {
        _uiState.update {
            val newEntry = it.powFactor.updateValue(value)
            val parsedValue = newEntry.value.toDoubleOrNullLocaleAware()
            val hasChanges = parsedValue != null && parsedValue != originalDifficultyAdjustmentFactor
            it.copy(
                powFactor = newEntry,
                hasChangesPowFactor = hasChanges,
            )
        }
    }

    private fun onPowFactorFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            _uiState.update {
                it.copy(powFactor = it.powFactor.validate())
            }
        }
    }

    private fun onPowFactorSave() {
        _uiState.update {
            it.copy(powFactor = it.powFactor.validate())
        }
        val currentEntry = _uiState.value.powFactor
        if (!currentEntry.isValid) return
        val parsedValue = currentEntry.value.toDoubleOrNullLocaleAware() ?: return

        guardedSuspendAction(_isPowFactorSaveEnabled, "onPowFactorSave") {
            settingsServiceFacade
                .setDifficultyAdjustmentFactor(parsedValue)
                .onSuccess {
                    originalDifficultyAdjustmentFactor = parsedValue
                    _uiState.update {
                        it.copy(hasChangesPowFactor = false)
                    }
                }.onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    private fun onPowFactorCancel() {
        _uiState.update {
            it.copy(
                powFactor = it.powFactor.updateValue(originalDifficultyAdjustmentFactor.toString()),
                hasChangesPowFactor = false,
            )
        }
    }

    private fun setIgnorePow(value: Boolean) {
        guardedSuspendAction(_isIgnorePowChangeEnabled, "setIgnorePow") {
            _uiState.update { it.copy(ignorePow = value) }
            settingsServiceFacade
                .setIgnoreDiffAdjustmentFromSecManager(value)
                .onFailure { exception ->
                    _uiState.update { it.copy(ignorePow = !value) }
                    handleError(exception)
                }
        }
    }

    private fun onResetAllDontShowAgainClick() {
        guardedSuspendAction(_isResetAllDontShowAgainEnabled, "onResetAllDontShowAgainClick") {
            settingsServiceFacade
                .resetAllDontShowAgainFlags()
                .onSuccess {
                    showSnackbar("mobile.settings.resetFlagsSuccess".i18n())
                }.onFailure { exception ->
                    handleError(exception)
                }
        }
    }

    private fun fetchSettings() {
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingSettings = true,
                    isFetchingSettingsError = false,
                )
            }

            settingsServiceFacade
                .getSettings()
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isFetchingSettings = false,
                            isFetchingSettingsError = true,
                        )
                    }
                }.onSuccess { settings ->
                    try {
                        val supportedLangCodes = settings.supportedLanguageCodes.ifEmpty { setOf(DEFAULT_LANGUAGE_CODE) }
                        val tradePriceToleranceFormatted = NumberFormatter.format(settings.maxTradePriceDeviation * 100)
                        val numDaysFormatted = settings.numDaysAfterRedactingTradeData.toString()
                        val powFactorFormatted = settingsServiceFacade.difficultyAdjustmentFactor.value.toString()
                        val ignorePowValue = settingsServiceFacade.ignoreDiffAdjustmentFromSecManager.value

                        val localSettings = settingsRepository.fetch()

                        // Store original values for cancel operations (raw numeric values from settings)
                        originalMaxTradePriceDeviation = settings.maxTradePriceDeviation
                        originalNumDaysAfterRedactingTradeData = settings.numDaysAfterRedactingTradeData
                        originalDifficultyAdjustmentFactor = settingsServiceFacade.difficultyAdjustmentFactor.value

                        _uiState.update {
                            it.copy(
                                i18nPairs = languageServiceFacade.i18nPairs.value,
                                allLanguagePairs = languageServiceFacade.allPairs.value,
                                languageCode = settings.languageCode,
                                supportedLanguageCodes = supportedLangCodes,
                                closeOfferWhenTradeTaken = settings.closeMyOfferWhenTaken,
                                tradePriceTolerance = it.tradePriceTolerance.updateValue(tradePriceToleranceFormatted),
                                // Show effective state: forced off (and greyed) on low-spec devices,
                                // without mutating the stored preference. See #1293.
                                useAnimations = animationSettings.isEffectivelyEnabled(settings.useAnimations),
                                rememberOfferbookFilterPreferences = localSettings.rememberOfferbookFilterPreferences,
                                numDaysAfterRedactingTradeData = it.numDaysAfterRedactingTradeData.updateValue(numDaysFormatted),
                                powFactor = it.powFactor.updateValue(powFactorFormatted),
                                ignorePow = ignorePowValue,
                                shouldShowPoWAdjustmentFactor = shouldShowPoWAdjustmentFactor,
                                isFetchingSettings = false,
                                isFetchingSettingsError = false,
                                // Reset hasChanges flags since we just loaded original values
                                hasChangesTradePriceTolerance = false,
                                hasChangesNumDaysAfterRedactingTradeData = false,
                                hasChangesPowFactor = false,
                            )
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        _uiState.update {
                            it.copy(
                                isFetchingSettings = false,
                                isFetchingSettingsError = true,
                            )
                        }
                        handleError(exception)
                    }
                }
        }
    }
}

private fun createTradePriceToleranceDataEntry(value: String): DataEntry =
    DataEntry(
        value = value,
        validator = { it ->
            if (it.isEmpty()) {
                "mobile.validation.valueCannotBeEmpty".i18n()
            } else {
                val parsedValue = it.toDoubleOrNullLocaleAware()
                if (parsedValue == null || parsedValue < 1 || parsedValue > 10) {
                    "settings.trade.maxTradePriceDeviation.invalid".i18n(1, 10)
                } else {
                    null
                }
            }
        },
    )

private fun createNumDaysAfterRedactingTradeDataDataEntry(value: String): DataEntry =
    DataEntry(
        value = value,
        validator = { it ->
            if (it.isEmpty()) {
                "mobile.validation.valueCannotBeEmpty".i18n()
            } else {
                val parsedValue = it.toIntOrNull()
                if (parsedValue == null || parsedValue < 30 || parsedValue > 365) {
                    "settings.trade.numDaysAfterRedactingTradeData.invalid".i18n(30, 365)
                } else {
                    null
                }
            }
        },
    )

private fun createPowFactorDataEntry(value: String): DataEntry =
    DataEntry(
        value = value,
        validator = { it ->
            if (it.isEmpty()) {
                "mobile.validation.valueCannotBeEmpty".i18n()
            } else {
                val parsedValue = it.toDoubleOrNullLocaleAware()
                if (parsedValue == null || parsedValue < 0 || parsedValue > 160_000) {
                    "authorizedRole.securityManager.difficultyAdjustment.invalid".i18n(160000)
                } else {
                    null
                }
            }
        },
    )
