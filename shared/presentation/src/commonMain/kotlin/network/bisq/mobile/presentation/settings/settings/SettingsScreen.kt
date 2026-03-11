package network.bisq.mobile.presentation.settings.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
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
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun SettingsScreen() {
    val presenter: SettingsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    SettingsContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.settings.title".i18n()) },
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
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
                    )

                    BisqHDivider()

                    BisqText.H4Light("settings.trade.headline".i18n())

                    BisqGap.V1()

                    BisqSwitch(
                        label = "settings.trade.closeMyOfferWhenTaken".i18n(),
                        checked = uiState.closeOfferWhenTradeTaken,
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
                                    )
                                }
                            } else {
                                null
                            },
                    )

                    BisqHDivider()

                    BisqText.H4Light("settings.display.headline".i18n())

                    BisqGap.V1()

                    BisqSwitch(
                        label = "settings.display.useAnimations".i18n(),
                        checked = uiState.useAnimations,
                        onSwitch = { onAction(SettingsUiAction.OnUseAnimationsChange(it)) },
                    )

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
                            onSwitch = { onAction(SettingsUiAction.OnIgnorePowChange(it)) },
                        )
                    }
                }
            }
        }
    }
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
