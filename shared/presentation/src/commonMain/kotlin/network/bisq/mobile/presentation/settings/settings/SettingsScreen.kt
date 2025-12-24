package network.bisq.mobile.presentation.settings.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqChipType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqMultiSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface IGeneralSettingsPresenter : ViewPresenter {
    val i18nPairs: StateFlow<Map<String, String>>
    val allLanguagePairs: StateFlow<Map<String, String>>

    val languageCode: StateFlow<String>

    fun setLanguageCode(langCode: String)

    val supportedLanguageCodes: StateFlow<Set<String>>

    fun setSupportedLanguageCodes(langCodes: Set<String>)

    val chatNotification: StateFlow<String>

    fun setChatNotification(value: String)

    val closeOfferWhenTradeTaken: StateFlow<Boolean>

    fun setCloseOfferWhenTradeTaken(value: Boolean)

    val tradePriceTolerance: StateFlow<String>

    fun setTradePriceTolerance(
        value: String,
        isValid: Boolean,
    )

    val useAnimations: StateFlow<Boolean>

    fun setUseAnimations(value: Boolean)

    val numDaysAfterRedactingTradeData: StateFlow<String>

    fun setNumDaysAfterRedactingTradeData(
        value: String,
        isValid: Boolean,
    )

    val powFactor: StateFlow<String>

    fun setPowFactor(
        value: String,
        isValid: Boolean,
    )

    val ignorePow: StateFlow<Boolean>

    fun setIgnorePow(value: Boolean)

    val shouldShowPoWAdjustmentFactor: StateFlow<Boolean>
}

@Composable
fun SettingsScreen() {
    val presenter: IGeneralSettingsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val i18nPairs by presenter.i18nPairs.collectAsState()
    val allLanguagePairs by presenter.allLanguagePairs.collectAsState()
    val selectedLanguage by presenter.languageCode.collectAsState()
    val supportedLanguageCodes by presenter.supportedLanguageCodes.collectAsState()
    val closeOfferWhenTradeTaken by presenter.closeOfferWhenTradeTaken.collectAsState()
    val tradePriceTolerance by presenter.tradePriceTolerance.collectAsState()
    val numDaysAfterRedactingTradeData by presenter.numDaysAfterRedactingTradeData.collectAsState()
    val useAnimations by presenter.useAnimations.collectAsState()
    val powFactor by presenter.powFactor.collectAsState()
    val ignorePow by presenter.ignorePow.collectAsState()
    val shouldShowPoWAdjustmentFactor by presenter.shouldShowPoWAdjustmentFactor.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Make the title reactive to language changes
        val title by remember(selectedLanguage) {
            derivedStateOf { "mobile.settings.title".i18n() }
        }

        BisqScrollScaffold(
            topBar = { TopBar(title) },
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            isInteractive = isInteractive,
        ) {
            BisqText.H4Light("settings.language".i18n())

            BisqGap.V1()

            BisqSelect(
                label = "settings.language.headline".i18n(),
                options = i18nPairs.entries,
                optionKey = { it.key },
                optionLabel = { it.value },
                selectedKey = selectedLanguage,
                disabled = !isInteractive,
                onSelect = { presenter.setLanguageCode(it.key) },
                searchable = true,
            )

            BisqMultiSelect(
                label = "settings.language.supported.headline".i18n(),
                helpText = "settings.language.supported.subHeadLine".i18n(),
                options = allLanguagePairs.entries,
                optionKey = { it.key },
                disabled = !isInteractive,
                optionLabel = { it.value },
                selectedKeys = supportedLanguageCodes,
                onSelectionChange = { option, selected ->
                    val current = presenter.supportedLanguageCodes.value
                    val next = if (selected) current + option.key else current - option.key
                    presenter.setSupportedLanguageCodes(next)
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
                checked = closeOfferWhenTradeTaken,
                onSwitch = { presenter.setCloseOfferWhenTradeTaken(it) },
            )

            BisqGap.V1()

            BisqTextField(
                label = "settings.trade.maxTradePriceDeviation".i18n(),
                value = tradePriceTolerance,
                keyboardType = KeyboardType.Decimal,
                onValueChange = { it, isValid -> presenter.setTradePriceTolerance(it, isValid) },
                helperText = "settings.trade.maxTradePriceDeviation.help".i18n(),
                numberWithTwoDecimals = true,
                valueSuffix = "%",
                validation = {
                    val parsedValue = it.toDoubleOrNullLocaleAware()
                    if (parsedValue == null) {
                        return@BisqTextField "mobile.settings.trade.maxTradePriceDeviation.validation.cannotBeEmpty".i18n()
                    }
                    if (parsedValue < 1 || parsedValue > 10) {
                        return@BisqTextField "settings.trade.maxTradePriceDeviation.invalid".i18n(1, 10)
                    }
                    return@BisqTextField null
                },
            )

            BisqGap.V1()

            BisqTextField(
                label = "settings.trade.numDaysAfterRedactingTradeData".i18n(),
                value = numDaysAfterRedactingTradeData,
                keyboardType = KeyboardType.Number,
                onValueChange = { it, isValid -> presenter.setNumDaysAfterRedactingTradeData(it, isValid) },
                helperText = "settings.trade.numDaysAfterRedactingTradeData.help".i18n(),
                validation = {
                    val parsedValue =
                        it.toDoubleOrNullLocaleAware()
                            ?: return@BisqTextField "mobile.settings.trade.numDaysAfterRedactingTradeData.validation.cannotBeEmpty".i18n()
                    if (parsedValue < 30 || parsedValue > 365) {
                        return@BisqTextField "settings.trade.numDaysAfterRedactingTradeData.invalid".i18n(30, 365)
                    }
                    return@BisqTextField null
                },
            )

            BisqHDivider()

            BisqText.H4Light("settings.display.headline".i18n())

            BisqGap.V1()

            BisqSwitch(
                label = "settings.display.useAnimations".i18n(),
                checked = useAnimations,
                onSwitch = { presenter.setUseAnimations(it) },
            )

            if (shouldShowPoWAdjustmentFactor) {
                BisqHDivider()

                BisqText.H4Light("settings.network.difficultyAdjustmentFactor.headline".i18n())

                BisqGap.V1()

                BisqTextField(
                    label = "settings.network.difficultyAdjustmentFactor.description.self".i18n(),
                    value = powFactor,
                    keyboardType = KeyboardType.Decimal,
                    disabled = !ignorePow,
                    onValueChange = { it, isValid -> presenter.setPowFactor(it, isValid) },
                    validation = {
                        val parsedValue =
                            it.toIntOrNull()
                                ?: return@BisqTextField "mobile.settings.network.difficultyAdjustmentFactor.validation.cannotBeEmpty".i18n()
                        if (parsedValue < 0 || parsedValue > 160_000) {
                            return@BisqTextField "authorizedRole.securityManager.difficultyAdjustment.invalid".i18n(
                                160000,
                            )
                        }
                        return@BisqTextField null
                    },
                )

                BisqGap.V1()

                BisqSwitch(
                    label = "settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager".i18n(),
                    checked = ignorePow,
                    onSwitch = { presenter.setIgnorePow(it) },
                )
            }
        }

        if (!isInteractive) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(60.dp),
                color = BisqTheme.colors.primary,
                strokeWidth = 1.dp,
            )
        }
    }
}
