package network.bisq.mobile.presentation.settings.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for SettingsContent using Robolectric.
 *
 * These tests verify that the SettingsContent composable renders correctly
 * for different UI states and that user interactions trigger the appropriate actions.
 */
@RunWith(AndroidJUnit4::class)
class SettingsContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockOnAction: (SettingsUiAction) -> Unit

    // Test data
    private val i18nPairs =
        mapOf(
            "en" to "English",
            "es" to "Spanish",
            "de" to "Deutsch",
        )

    private val allLanguagePairs =
        mapOf(
            "en" to "English",
            "es" to "Spanish",
            "de" to "Deutsch",
            "fr" to "Français",
            "pt" to "Português",
        )

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        mockOnAction = mockk(relaxed = true)
    }

    /**
     * Helper function to set up test content with LocalIsTest enabled.
     * Wraps content with CompositionLocalProvider and BisqTheme to avoid repetition.
     */
    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    // ========== Loading State Tests ==========

    @Test
    fun `when loading state renders then shows loading indicator`() {
        // Given
        val uiState =
            SettingsUiState(
                tradePriceTolerance = DataEntry(value = ""),
                numDaysAfterRedactingTradeData = DataEntry(value = ""),
                powFactor = DataEntry(value = ""),
                isFetchingSettings = true,
            )

        // When
        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then - LoadingState with CircularProgressIndicator should be displayed
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
    }

    // ========== Error State Tests ==========

    @Test
    fun `when error state renders then shows error message and retry button`() {
        // Given
        val uiState =
            SettingsUiState(
                tradePriceTolerance = DataEntry(value = ""),
                numDaysAfterRedactingTradeData = DataEntry(value = ""),
                powFactor = DataEntry(value = ""),
                isFetchingSettingsError = true,
                isFetchingSettings = false,
            )

        // When
        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then - ErrorState with title and error message should be displayed
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.error.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.error.generic".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when error state and retry clicked then triggers OnRetryLoadSettingsClick action`() {
        // Given
        val uiState =
            SettingsUiState(
                tradePriceTolerance = DataEntry(value = ""),
                numDaysAfterRedactingTradeData = DataEntry(value = ""),
                powFactor = DataEntry(value = ""),
                isFetchingSettingsError = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Click retry button (ErrorState uses "mobile.action.retry" text)
        composeTestRule
            .onNodeWithText("mobile.action.retry".i18n())
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnRetryLoadSettingsClick) }
    }

    // ========== Normal Content - Language Tests ==========

    @Test
    fun `when normal state renders then shows language settings`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                allLanguagePairs = allLanguagePairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en", "es"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                useAnimations = true,
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        // When
        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("settings.language".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.language.headline".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.language.supported.headline".i18n())
            .assertIsDisplayed()
    }

    // ========== Normal Content - Trade Settings Tests ==========

    @Test
    fun `when normal state renders then shows trade settings`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        // When
        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("settings.trade.headline".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.trade.closeMyOfferWhenTaken".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.trade.maxTradePriceDeviation".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.trade.numDaysAfterRedactingTradeData".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when close offer switch clicked then triggers OnCloseOfferWhenTradeTakenChange action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = false,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Click on the switch (find by the label text)
        composeTestRule
            .onNodeWithText("settings.trade.closeMyOfferWhenTaken".i18n())
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnCloseOfferWhenTradeTakenChange(true)) }
    }

    // ========== Trade Price Tolerance Tests ==========

    @Test
    fun `when trade price tolerance changed then triggers OnTradePriceToleranceChange action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Enter text in the price tolerance field
        composeTestRule
            .onNodeWithText("5")
            .performTextReplacement("10")

        // Then
        verify { mockOnAction(SettingsUiAction.OnTradePriceToleranceChange("10")) }
    }

    @Test
    fun `when trade price tolerance has changes then shows save and cancel buttons`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "10"),
                hasChangesTradePriceTolerance = true,
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        // EditableFieldActions shows both save and cancel buttons
        composeTestRule
            .onNodeWithContentDescription("save")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("cancel")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when trade price tolerance save clicked then triggers OnTradePriceToleranceSave action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "10"),
                hasChangesTradePriceTolerance = true,
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Click save button
        composeTestRule
            .onNodeWithContentDescription("save")
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnTradePriceToleranceSave) }
    }

    @Test
    fun `when trade price tolerance cancel clicked then triggers OnTradePriceToleranceCancel action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "10"),
                hasChangesTradePriceTolerance = true,
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Click cancel button
        composeTestRule
            .onNodeWithContentDescription("cancel")
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnTradePriceToleranceCancel) }
    }

    @Test
    fun `when trade price tolerance has validation error then shows error message`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance =
                    DataEntry(
                        value = "invalid",
                        errorMessage = "Invalid number format",
                    ),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Invalid number format")
            .assertIsDisplayed()
    }

    // ========== Num Days After Redacting Trade Data Tests ==========

    @Test
    fun `when num days after redacting changed then triggers OnNumDaysAfterRedactingTradeDataChange action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Enter text in the num days field
        composeTestRule
            .onNodeWithText("90")
            .performTextReplacement("120")

        // Then
        verify { mockOnAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("120")) }
    }

    @Test
    fun `when num days has changes then shows save and cancel buttons`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "120"),
                hasChangesNumDaysAfterRedactingTradeData = true,
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("save")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("cancel")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when num days save clicked then triggers OnNumDaysAfterRedactingTradeDataSave action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "120"),
                hasChangesNumDaysAfterRedactingTradeData = true,
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithContentDescription("save")
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataSave) }
    }

    @Test
    fun `when num days cancel clicked then triggers OnNumDaysAfterRedactingTradeDataCancel action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "120"),
                hasChangesNumDaysAfterRedactingTradeData = true,
                powFactor = DataEntry(value = "1"),
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithContentDescription("cancel")
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataCancel) }
    }

    // ========== Display Settings Tests ==========

    @Test
    fun `when normal state renders then shows display settings`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                useAnimations = true,
                isFetchingSettings = false,
            )

        // When
        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("settings.display.headline".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.display.useAnimations".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when use animations switch clicked then triggers OnUseAnimationsChange action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                useAnimations = false,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("settings.display.useAnimations".i18n())
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnUseAnimationsChange(true)) }
    }

    // ========== PoW Adjustment Factor Tests ==========

    @Test
    fun `when should show pow adjustment is true then shows pow settings`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                ignorePow = false,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        // When
        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("settings.network.difficultyAdjustmentFactor.headline".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.network.difficultyAdjustmentFactor.description.self".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when pow factor changed then triggers OnPowFactorChange action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                ignorePow = true,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("1")
            .performTextReplacement("2")

        // Then
        verify { mockOnAction(SettingsUiAction.OnPowFactorChange("2")) }
    }

    @Test
    fun `when pow factor save clicked then triggers OnPowFactorSave action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "2"),
                hasChangesPowFactor = true,
                ignorePow = true,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithContentDescription("save")
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnPowFactorSave) }
    }

    @Test
    fun `when pow factor cancel clicked then triggers OnPowFactorCancel action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "2"),
                hasChangesPowFactor = true,
                ignorePow = true,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithContentDescription("cancel")
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnPowFactorCancel) }
    }

    @Test
    fun `when ignore pow switch clicked then triggers OnIgnorePowChange action`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                ignorePow = false,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager".i18n())
            .performScrollTo()
            .performClick()

        // Then
        verify { mockOnAction(SettingsUiAction.OnIgnorePowChange(true)) }
    }

    @Test
    fun `when ignore pow is false then pow factor field is disabled`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                ignorePow = false,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // Then - The text field should be disabled when ignorePow is false
        composeTestRule
            .onNodeWithText("1")
            .assertIsNotEnabled()
    }

    @Test
    fun `when ignore pow is true then pow factor field is enabled`() {
        // Given
        val uiState =
            SettingsUiState(
                i18nPairs = i18nPairs,
                languageCode = "en",
                supportedLanguageCodes = setOf("en"),
                closeOfferWhenTradeTaken = true,
                tradePriceTolerance = DataEntry(value = "5"),
                numDaysAfterRedactingTradeData = DataEntry(value = "90"),
                powFactor = DataEntry(value = "1"),
                ignorePow = true,
                shouldShowPoWAdjustmentFactor = true,
                useAnimations = true,
                isFetchingSettings = false,
            )

        setTestContent {
            SettingsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // Then - The text field should be enabled when ignorePow is true
        composeTestRule
            .onNodeWithText("1")
            .assertIsEnabled()
    }
}
