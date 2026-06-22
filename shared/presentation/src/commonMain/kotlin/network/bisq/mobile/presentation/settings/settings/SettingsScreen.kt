package network.bisq.mobile.presentation.settings.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqChipType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqMultiSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.EditableFieldActions
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.common.ui.utils.rememberNotificationPermissionLauncher
import org.koin.compose.koinInject

@Composable
fun SettingsScreen() {
    val presenter: SettingsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()
    val isTradePriceToleranceSaveEnabled by presenter.isTradePriceToleranceSaveEnabled.collectAsState()
    val isNumDaysAfterRedactingTradeDataSaveEnabled by presenter.isNumDaysAfterRedactingTradeDataSaveEnabled.collectAsState()
    val isPowFactorSaveEnabled by presenter.isPowFactorSaveEnabled.collectAsState()
    val isPushNotificationsToggleEnabled by presenter.isPushNotificationsToggleEnabled.collectAsState()
    val isLanguageCodeChangeEnabled by presenter.isLanguageCodeChangeEnabled.collectAsState()
    val isSupportedLanguageCodesChangeEnabled by presenter.isSupportedLanguageCodesChangeEnabled.collectAsState()
    val isCloseOfferWhenTradeTakenChangeEnabled by presenter.isCloseOfferWhenTradeTakenChangeEnabled.collectAsState()
    val isUseAnimationsChangeEnabled by presenter.isUseAnimationsChangeEnabled.collectAsState()
    val isIgnorePowChangeEnabled by presenter.isIgnorePowChangeEnabled.collectAsState()
    val isResetAllDontShowAgainEnabled by presenter.isResetAllDontShowAgainEnabled.collectAsState()

    SettingsContent(
        uiState = uiState,
        isTradePriceToleranceSaveEnabled = isTradePriceToleranceSaveEnabled,
        isNumDaysAfterRedactingTradeDataSaveEnabled = isNumDaysAfterRedactingTradeDataSaveEnabled,
        isPowFactorSaveEnabled = isPowFactorSaveEnabled,
        isPushNotificationsToggleEnabled = isPushNotificationsToggleEnabled,
        isLanguageCodeChangeEnabled = isLanguageCodeChangeEnabled,
        isSupportedLanguageCodesChangeEnabled = isSupportedLanguageCodesChangeEnabled,
        isCloseOfferWhenTradeTakenChangeEnabled = isCloseOfferWhenTradeTakenChangeEnabled,
        isUseAnimationsChangeEnabled = isUseAnimationsChangeEnabled,
        isIgnorePowChangeEnabled = isIgnorePowChangeEnabled,
        isResetAllDontShowAgainEnabled = isResetAllDontShowAgainEnabled,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.settings.title".i18n()) },
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    isTradePriceToleranceSaveEnabled: Boolean = true,
    isNumDaysAfterRedactingTradeDataSaveEnabled: Boolean = true,
    isPowFactorSaveEnabled: Boolean = true,
    isPushNotificationsToggleEnabled: Boolean = true,
    isLanguageCodeChangeEnabled: Boolean = true,
    isSupportedLanguageCodesChangeEnabled: Boolean = true,
    isCloseOfferWhenTradeTakenChangeEnabled: Boolean = true,
    isUseAnimationsChangeEnabled: Boolean = true,
    isIgnorePowChangeEnabled: Boolean = true,
    isResetAllDontShowAgainEnabled: Boolean = true,
    topBar: @Composable () -> Unit = {},
) {
    // Pre-prompt explainer + system permission launcher for the push-notifications
    // opt-in toggle. The runtime POST_NOTIFICATIONS prompt (Android 13+) needs an
    // Activity context, so we launch it here rather than from the presenter.
    // On grant we dispatch the toggle-on action; on denial the switch stays off
    // (`uiState.pushNotificationsEnabled` is the source of truth and only flips
    // after the facade actually registers).
    var showPushPermissionExplainer by remember { mutableStateOf(false) }
    val notifPermissionLauncher =
        rememberNotificationPermissionLauncher { granted ->
            if (granted) {
                onAction(SettingsUiAction.OnPushNotificationsToggle(true))
            }
        }

    BisqScaffold(
        topBar = topBar,
    ) { paddingValues ->

        when {
            uiState.isFetchingSettings -> {
                LoadingState(paddingValues)
            }

            uiState.isFetchingSettingsError -> {
                ErrorState(
                    paddingValues = paddingValues,
                    onRetry = { onAction(SettingsUiAction.OnRetryLoadSettingsClick) },
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(paddingValues)
                            .padding(16.dp),
                ) {
                    BisqText.H4Light("settings.language".i18n())

                    BisqGap.V1()

                    BisqSelect(
                        label = "settings.language.headline".i18n(),
                        options = uiState.i18nPairs.entries,
                        optionKey = { it.key },
                        optionLabel = { it.value },
                        selectedKey = uiState.languageCode,
                        onSelect = { onAction(SettingsUiAction.OnLanguageCodeChange(it.key)) },
                        searchable = true,
                        disabled = !isLanguageCodeChangeEnabled,
                    )

                    BisqMultiSelect(
                        label = "settings.language.supported.headline".i18n(),
                        helpText = "settings.language.supported.subHeadLine".i18n(),
                        options = uiState.allLanguagePairs.entries,
                        optionKey = { it.key },
                        optionLabel = { it.value },
                        selectedKeys = uiState.supportedLanguageCodes,
                        onSelectionChange = { option, selected ->
                            onAction(SettingsUiAction.OnSupportedLanguageCodeToggle(option.key, selected))
                        },
                        searchable = true,
                        maxSelectionLimit = 5,
                        minSelectionLimit = 1,
                        chipType = BisqChipType.Outline,
                        disabled = !isSupportedLanguageCodesChangeEnabled,
                    )

                    BisqHDivider()

                    BisqText.H4Light("settings.trade.headline".i18n())

                    BisqGap.V1()

                    BisqSwitch(
                        label = "settings.trade.closeMyOfferWhenTaken".i18n(),
                        checked = uiState.closeOfferWhenTradeTaken,
                        disabled = !isCloseOfferWhenTradeTakenChangeEnabled,
                        onSwitch = { onAction(SettingsUiAction.OnCloseOfferWhenTradeTakenChange(it)) },
                    )

                    BisqGap.V1()

                    BisqTextFieldV0(
                        label = "settings.trade.maxTradePriceDeviation".i18n(),
                        value = uiState.tradePriceTolerance.value,
                        onValueChange = {
                            onAction(SettingsUiAction.OnTradePriceToleranceChange(it))
                        },
                        modifier =
                            Modifier
                                .onFocusChanged { focusState ->
                                    onAction(SettingsUiAction.OnTradePriceToleranceFocus(focusState.isFocused))
                                },
                        isError = !uiState.tradePriceTolerance.isValid,
                        bottomMessage = if (uiState.tradePriceTolerance.isValid) "settings.trade.maxTradePriceDeviation.help".i18n() else uiState.tradePriceTolerance.errorMessage,
                        placeholder = "settings.trade.maxTradePriceDeviation.help".i18n(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        trailingIcon =
                            if (uiState.hasChangesTradePriceTolerance) {
                                {
                                    EditableFieldActions(
                                        onSave = { onAction(SettingsUiAction.OnTradePriceToleranceSave) },
                                        onCancel = { onAction(SettingsUiAction.OnTradePriceToleranceCancel) },
                                        disabled = !isTradePriceToleranceSaveEnabled,
                                    )
                                }
                            } else {
                                null
                            },
                        suffix = {
                            BisqText.BaseLight(
                                text = "%",
                            )
                        },
                    )

                    BisqGap.V1()

                    BisqTextFieldV0(
                        label = "settings.trade.numDaysAfterRedactingTradeData".i18n(),
                        value = uiState.numDaysAfterRedactingTradeData.value,
                        onValueChange = {
                            onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange(it))
                        },
                        modifier =
                            Modifier
                                .onFocusChanged { focusState ->
                                    onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataFocus(focusState.isFocused))
                                },
                        isError = !uiState.numDaysAfterRedactingTradeData.isValid,
                        bottomMessage = if (uiState.numDaysAfterRedactingTradeData.isValid)"settings.trade.numDaysAfterRedactingTradeData.help".i18n() else uiState.numDaysAfterRedactingTradeData.errorMessage,
                        placeholder = "settings.trade.numDaysAfterRedactingTradeData.help".i18n(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon =
                            if (uiState.hasChangesNumDaysAfterRedactingTradeData) {
                                {
                                    EditableFieldActions(
                                        onSave = { onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataSave) },
                                        onCancel = { onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataCancel) },
                                        disabled = !isNumDaysAfterRedactingTradeDataSaveEnabled,
                                    )
                                }
                            } else {
                                null
                            },
                    )

                    BisqHDivider()
                    AnalyticsSection(uiState, onAction)

                    BisqHDivider()
                    BisqText.H4Light("settings.display.headline".i18n())

                    BisqGap.V1()

                    BisqSwitch(
                        label = "settings.display.useAnimations".i18n(),
                        checked = uiState.useAnimations,
                        disabled = !isUseAnimationsChangeEnabled,
                        onSwitch = { onAction(SettingsUiAction.OnUseAnimationsChange(it)) },
                    )

                    BisqGap.V1()
                    BisqButton(
                        text = "settings.display.resetDontShowAgain".i18n(),
                        onClick = { onAction(SettingsUiAction.OnResetAllDontShowAgainClick) },
                        type = BisqButtonType.Outline,
                        fullWidth = true,
                        disabled = !isResetAllDontShowAgainEnabled,
                    )

                    BisqHDivider()

                    BisqText.H4Light("settings.offerbook.headline".i18n())

                    BisqGap.V1()

                    BisqSwitch(
                        label = "settings.offerbook.rememberFilterPreferences".i18n(),
                        checked = uiState.rememberOfferbookFilterPreferences,
                        onSwitch = { onAction(SettingsUiAction.OnRememberOfferbookFilterPreferencesChange(it)) },
                    )

                    BisqGap.VQuarter()

                    BisqText.SmallLight(
                        text = "settings.offerbook.rememberFilterPreferences.help".i18n(),
                        color = BisqTheme.colors.mid_grey20,
                    )

                    if (uiState.shouldShowPushNotificationsToggle) {
                        BisqHDivider()

                        BisqText.H4Light("mobile.pushNotifications.settings.title".i18n())

                        BisqGap.V1()

                        BisqSwitch(
                            label = "mobile.pushNotifications.settings.toggleLabel".i18n(),
                            checked = uiState.pushNotificationsEnabled,
                            disabled = !isPushNotificationsToggleEnabled,
                            onSwitch = { newValue ->
                                if (newValue) {
                                    showPushPermissionExplainer = true
                                } else {
                                    onAction(SettingsUiAction.OnPushNotificationsToggle(false))
                                }
                            },
                        )

                        BisqGap.VQuarter()

                        BisqText.SmallLight(
                            text = "mobile.pushNotifications.settings.subtitle".i18n(),
                            color = BisqTheme.colors.mid_grey20,
                        )

                        BisqGap.VHalf()

                        BisqText.SmallLight(
                            text = "mobile.pushNotifications.settings.learnMoreLink".i18n(),
                            color = BisqTheme.colors.primary,
                            modifier =
                                Modifier.clickable {
                                    onAction(SettingsUiAction.OnPushNotificationsLearnMore)
                                },
                        )

                        PushNotificationsExtraGuidance(uiState, onAction)

                        if (showPushPermissionExplainer) {
                            ConfirmationDialog(
                                headline = "mobile.pushNotifications.optIn.headline".i18n(),
                                message = "mobile.pushNotifications.optIn.body".i18n(),
                                confirmButtonText = "mobile.pushNotifications.optIn.confirm".i18n(),
                                dismissButtonText = "mobile.pushNotifications.optIn.cancel".i18n(),
                                verticalButtonPlacement = true,
                                horizontalAlignment = Alignment.Start,
                                extraContent = {
                                    BisqText.SmallLight(
                                        text = "mobile.pushNotifications.settings.learnMoreLink".i18n(),
                                        color = BisqTheme.colors.primary,
                                        modifier =
                                            Modifier.clickable {
                                                onAction(SettingsUiAction.OnPushNotificationsLearnMore)
                                            },
                                    )
                                },
                                onConfirm = {
                                    showPushPermissionExplainer = false
                                    notifPermissionLauncher.launch()
                                },
                                onDismiss = {
                                    showPushPermissionExplainer = false
                                },
                            )
                        }
                    }

                    if (uiState.shouldShowPoWAdjustmentFactor) {
                        BisqHDivider()

                        BisqText.H4Light("settings.network.difficultyAdjustmentFactor.headline".i18n())

                        BisqGap.V1()

                        BisqTextFieldV0(
                            label = "settings.network.difficultyAdjustmentFactor.description.self".i18n(),
                            value = uiState.powFactor.value,
                            enabled = uiState.ignorePow,
                            onValueChange = {
                                onAction(SettingsUiAction.OnPowFactorChange(it))
                            },
                            modifier =
                                Modifier
                                    .onFocusChanged { focusState ->
                                        onAction(SettingsUiAction.OnPowFactorFocus(focusState.isFocused))
                                    },
                            isError = !uiState.powFactor.isValid,
                            bottomMessage = uiState.powFactor.errorMessage,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon =
                                if (uiState.hasChangesPowFactor) {
                                    {
                                        EditableFieldActions(
                                            onSave = { onAction(SettingsUiAction.OnPowFactorSave) },
                                            onCancel = { onAction(SettingsUiAction.OnPowFactorCancel) },
                                            disabled = !isPowFactorSaveEnabled,
                                        )
                                    }
                                } else {
                                    null
                                },
                        )

                        BisqGap.V1()

                        BisqSwitch(
                            label = "settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager".i18n(),
                            checked = uiState.ignorePow,
                            disabled = !isIgnorePowChangeEnabled,
                            onSwitch = { onAction(SettingsUiAction.OnIgnorePowChange(it)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tail of the push-notifications settings section: either the "keep connected
 * in background" sub-toggle (when relayed is on AND the platform supports it)
 * or the legacy "you may miss messages" warning (when relayed is off).
 *
 * Extracted from [SettingsScreen] so it can be excluded from coverage —
 * declarative Compose with no logic worth a unit test; the underlying gating
 * lives in [SettingsPresenter] and is covered there.
 */
@ExcludeFromCoverage
@Composable
private fun PushNotificationsExtraGuidance(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
) {
    if (uiState.pushNotificationsEnabled && uiState.shouldShowKeepConnectedToggle) {
        // V1 gap above gives the dependency relationship room to breathe.
        BisqGap.V1()
        KeepConnectedSetting(
            enabled = uiState.keepConnectedInBackground,
            onToggle = { newValue ->
                onAction(SettingsUiAction.OnKeepConnectedInBackgroundToggle(newValue))
            },
        )
    } else if (!uiState.pushNotificationsEnabled) {
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = "mobile.pushNotifications.settings.disabledWarning".i18n(),
            color = BisqTheme.colors.warning,
        )
    }
}

/**
 * Crash & usage reporting (analytics) section. Two halves:
 *  1. Title + toggle switch + subtitle (mirrors push notifications pattern).
 *  2. Privacy bullets + "Learn more" link routing to the analytics wiki page.
 *
 * Visibility is unconditional — the privacy contract is opt-in everywhere, so
 * every user gets to see this section and decide. The toggle persists through
 * [SettingsPresenter.onAnalyticsToggle] → [SettingsRepository.setAnalyticsEnabled]
 * → DI's `runtimeOptInProvider` StateFlow on the next emit.
 *
 * Excluded from coverage like the other declarative Compose helpers in this
 * file — the underlying logic lives in the presenter and is unit-tested there.
 */
@ExcludeFromCoverage
@Composable
private fun AnalyticsSection(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
) {
    BisqText.H4Light("mobile.settings.analytics.title".i18n())

    BisqGap.V1()

    BisqSwitch(
        label = "mobile.settings.analytics.toggleLabel".i18n(),
        checked = uiState.analyticsEnabled,
        onSwitch = { onAction(SettingsUiAction.OnAnalyticsToggle(it)) },
    )

    BisqGap.VQuarter()

    BisqText.SmallLight(
        text = "mobile.settings.analytics.subtitle".i18n(),
        color = BisqTheme.colors.mid_grey20,
    )

    BisqGap.V1()

    // Privacy bullets — four short lines summarising the wiki contract for
    // users who won't tap "Learn more". Kept in i18n so translators can adapt
    // wording per language.
    AnalyticsBulletRow("mobile.settings.analytics.info.tor".i18n())
    AnalyticsBulletRow("mobile.settings.analytics.info.noPii".i18n())
    AnalyticsBulletRow("mobile.settings.analytics.info.noTrade".i18n())
    AnalyticsBulletRow("mobile.settings.analytics.info.retention".i18n())

    BisqGap.VHalf()

    BisqText.SmallLight(
        text = "mobile.settings.analytics.learnMore".i18n(),
        color = BisqTheme.colors.primary,
        modifier =
            Modifier.clickable {
                onAction(SettingsUiAction.OnAnalyticsLearnMore)
            },
    )
}

@ExcludeFromCoverage
@Composable
private fun AnalyticsBulletRow(text: String) {
    BisqText.SmallLight(
        text = "• $text",
        color = BisqTheme.colors.mid_grey20,
    )
    BisqGap.VQuarter()
}

@ExcludeFromCoverage
@Composable
private fun PreviewTopBar() {
    TopBarContent(
        title = "mobile.settings.title".i18n(),
        showBackButton = true,
        showUserAvatar = true,
    )
}

private val previewOnAction: (SettingsUiAction) -> Unit = {}

@Preview
@Composable
private fun SettingsScreen_TradePriceToleranceWithChangesPreview() {
    BisqTheme.Preview {
        SettingsContent(
            uiState =
                SettingsUiState(
                    i18nPairs =
                        mapOf(
                            "en" to "English",
                            "es" to "Spanish",
                            "de" to "Deutsch",
                        ),
                    allLanguagePairs =
                        mapOf(
                            "en" to "English",
                            "es" to "Spanish",
                            "de" to "Deutsch",
                            "fr" to "Français",
                            "pt" to "Português",
                        ),
                    languageCode = "en",
                    supportedLanguageCodes = setOf("en", "es"),
                    closeOfferWhenTradeTaken = true,
                    tradePriceTolerance = DataEntry(value = "10"),
                    hasChangesTradePriceTolerance = true,
                    numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                    powFactor = DataEntry(value = "1"),
                    ignorePow = false,
                    shouldShowPoWAdjustmentFactor = false,
                    useAnimations = true,
                    isFetchingSettings = false,
                ),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun SettingsScreen_Preview() {
    BisqTheme.Preview {
        SettingsContent(
            uiState =
                SettingsUiState(
                    i18nPairs =
                        mapOf(
                            "en" to "English",
                            "es" to "Spanish",
                            "de" to "Deutsch",
                        ),
                    allLanguagePairs =
                        mapOf(
                            "en" to "English",
                            "es" to "Spanish",
                            "de" to "Deutsch",
                            "fr" to "Français",
                            "pt" to "Português",
                        ),
                    languageCode = "en",
                    supportedLanguageCodes = setOf("en", "es"),
                    closeOfferWhenTradeTaken = true,
                    tradePriceTolerance = DataEntry(value = "5"),
                    useAnimations = true,
                    numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                    powFactor = DataEntry(value = "1"),
                    ignorePow = false,
                    shouldShowPoWAdjustmentFactor = false,
                    isFetchingSettings = false,
                ),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}
