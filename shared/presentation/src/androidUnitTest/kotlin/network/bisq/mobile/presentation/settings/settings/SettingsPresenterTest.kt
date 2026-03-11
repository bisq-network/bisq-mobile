package network.bisq.mobile.presentation.settings.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.replicated.settings.DEFAULT_MAX_TRADE_PRICE_DEVIATION
import network.bisq.mobile.domain.data.replicated.settings.DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsPresenter.
 *
 * These tests verify the business logic of the SettingsPresenter,
 * including settings loading, language management, validation of numeric fields,
 * save/cancel operations, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private var originalLocale: Locale? = null

    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var languageServiceFacade: LanguageServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var presenter: SettingsPresenter

    // Test data
    private val sampleSettings =
        SettingsVO(
            isTacAccepted = true,
            tradeRulesConfirmed = true,
            closeMyOfferWhenTaken = true,
            languageCode = "en",
            supportedLanguageCodes = setOf("en", "es"),
            maxTradePriceDeviation = 0.05, // 5%
            useAnimations = true,
            selectedMarket = null,
            numDaysAfterRedactingTradeData = 90,
        )

    private val sampleI18nPairs = mapOf("key1" to "value1", "key2" to "value2")
    private val sampleAllPairs = mapOf("en" to "English", "es" to "Spanish", "de" to "German")

    @BeforeTest
    fun setUp() {
        // Force US locale for consistent decimal separator (period) in tests.
        // Tests use hardcoded values like "0.5" which would fail on comma-decimal locales.
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)

        Dispatchers.setMain(testDispatcher)

        // Setup mocks
        settingsServiceFacade = mockk(relaxed = true)
        languageServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                    single<GlobalUiManager> { globalUiManager }
                },
            )
        }

        // Default mock behaviors for StateFlows
        every { languageServiceFacade.i18nPairs } returns MutableStateFlow(sampleI18nPairs)
        every { languageServiceFacade.allPairs } returns MutableStateFlow(sampleAllPairs)
        every { settingsServiceFacade.difficultyAdjustmentFactor } returns MutableStateFlow(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR)
        every { settingsServiceFacade.ignoreDiffAdjustmentFromSecManager } returns MutableStateFlow(false)
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
            originalLocale?.let { Locale.setDefault(it) }
        }
    }

    private fun createPresenter(): SettingsPresenter =
        SettingsPresenter(
            settingsServiceFacade,
            languageServiceFacade,
            mainPresenter,
        )

    @Test
    fun `when initial state then has correct default values`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            // When
            presenter = createPresenter()

            // Then - before view attached, check initial DataEntry values are set
            val state = presenter.uiState.value
            assertFalse(state.isFetchingSettingsError)
            assertEquals(NumberFormatter.format(DEFAULT_MAX_TRADE_PRICE_DEVIATION * 100), state.tradePriceTolerance.value)
            assertEquals(DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA.toString(), state.numDaysAfterRedactingTradeData.value)
            assertEquals(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR.toString(), state.powFactor.value)
        }

    // ========== Settings Loading Tests ==========

    @Test
    fun `when loading settings succeeds then updates state correctly`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.getSettings() }
            val state = presenter.uiState.value
            assertFalse(state.isFetchingSettings)
            assertFalse(state.isFetchingSettingsError)
            assertEquals("en", state.languageCode)
            assertEquals(setOf("en", "es"), state.supportedLanguageCodes)
            assertTrue(state.closeOfferWhenTradeTaken)
            assertTrue(state.useAnimations)
            assertEquals(NumberFormatter.format(sampleSettings.maxTradePriceDeviation * 100), state.tradePriceTolerance.value) // 0.05 * 100 = 5%
            assertEquals(sampleSettings.numDaysAfterRedactingTradeData.toString(), state.numDaysAfterRedactingTradeData.value)
            assertEquals(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR.toString(), state.powFactor.value) // DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR = 1.0
        }

    @Test
    fun `when loading settings fails then sets error state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.failure(Exception("Network error"))

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.isFetchingSettings)
            assertTrue(state.isFetchingSettingsError)
        }

    @Test
    fun `when retry load settings clicked then reloads settings`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.failure(Exception("Error"))
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Setup successful response for retry
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            // When
            presenter.onAction(SettingsUiAction.OnRetryLoadSettingsClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { settingsServiceFacade.getSettings() }
            val state = presenter.uiState.value
            assertFalse(state.isFetchingSettings)
            assertFalse(state.isFetchingSettingsError)
        }

    // ========== Language Setting Tests ==========

    @Test
    fun `when language code changes then updates state and calls service`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setLanguageCode("de") } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnLanguageCodeChange("de"))
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setLanguageCode("de") }
            val state = presenter.uiState.value
            assertEquals("de", state.languageCode)
        }

    @Test
    fun `when setting language code fails then reverts state and shows error`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setLanguageCode("de") } returns Result.failure(Exception("Error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnLanguageCodeChange("de"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("en", state.languageCode) // Reverted to original
            coVerify { globalUiManager.showSnackbar("mobile.error.generic".i18n(), type = SnackbarType.ERROR, any()) }
        }

    @Test
    fun `when supported language toggled on then adds to set`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setSupportedLanguageCodes(setOf("en", "es", "de")) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnSupportedLanguageCodeToggle("de", true))
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setSupportedLanguageCodes(setOf("en", "es", "de")) }
            val state = presenter.uiState.value
            assertEquals(setOf("en", "es", "de"), state.supportedLanguageCodes)
        }

    @Test
    fun `when supported language toggled off then removes from set`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setSupportedLanguageCodes(setOf("en")) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnSupportedLanguageCodeToggle("es", false))
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setSupportedLanguageCodes(setOf("en")) }
            val state = presenter.uiState.value
            assertEquals(setOf("en"), state.supportedLanguageCodes)
        }

    // ========== Trade Settings Tests ==========

    @Test
    fun `when close offer when trade taken toggled then updates state and calls service`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setCloseMyOfferWhenTaken(false) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnCloseOfferWhenTradeTakenChange(false))
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setCloseMyOfferWhenTaken(false) }
            val state = presenter.uiState.value
            assertFalse(state.closeOfferWhenTradeTaken)
        }

    @Test
    fun `when setting close offer when trade taken fails then reverts state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setCloseMyOfferWhenTaken(false) } returns Result.failure(Exception("Error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnCloseOfferWhenTradeTakenChange(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.closeOfferWhenTradeTaken) // Reverted to original
        }

    // ========== Trade Price Tolerance Tests ==========

    @Test
    fun `when trade price tolerance changes to valid value then updates state and tracks changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - change from 5% to 7%
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("7"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("7", state.tradePriceTolerance.value)
            assertTrue(state.hasChangesTradePriceTolerance)
        }

    @Test
    fun `when trade price tolerance changes to same value then no changes tracked`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - change to same value (5%)
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("5"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("5", state.tradePriceTolerance.value)
            assertFalse(state.hasChangesTradePriceTolerance)
        }

    @Test
    fun `when trade price tolerance is below minimum then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("0.5"))
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.tradePriceTolerance.errorMessage)
        }

    @Test
    fun `when trade price tolerance is above maximum then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("15"))
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.tradePriceTolerance.errorMessage)
        }

    @Test
    fun `when trade price tolerance is empty then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange(""))
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.tradePriceTolerance.errorMessage)
        }

    @Test
    fun `when trade price tolerance save succeeds then updates original value and clears changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setMaxTradePriceDeviation(0.07) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("7"))
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceSave)
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setMaxTradePriceDeviation(0.07) } // 7% = 0.07
            val state = presenter.uiState.value
            assertFalse(state.hasChangesTradePriceTolerance)
        }

    @Test
    fun `when trade price tolerance cancel then restores original value`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("7"))
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.hasChangesTradePriceTolerance)

            // When
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceCancel)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            // Value should be formatted 5.0 (which could be "5.00" or "5,00" depending on locale)
            assertTrue(state.tradePriceTolerance.value.contains("5"))
            assertFalse(state.hasChangesTradePriceTolerance)
        }

    // ========== Num Days After Redacting Tests ==========

    @Test
    fun `when num days after redacting changes to valid value then updates state and tracks changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - change from 90 to 120 days
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("120"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("120", state.numDaysAfterRedactingTradeData.value)
            assertTrue(state.hasChangesNumDaysAfterRedactingTradeData)
        }

    @Test
    fun `when num days after redacting is below minimum then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("15"))
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.numDaysAfterRedactingTradeData.errorMessage)
        }

    @Test
    fun `when num days after redacting is above maximum then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("400"))
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.numDaysAfterRedactingTradeData.errorMessage)
        }

    @Test
    fun `when num days after redacting save succeeds then updates original value and clears changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setNumDaysAfterRedactingTradeData(120) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("120"))
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataSave)
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setNumDaysAfterRedactingTradeData(120) }
            val state = presenter.uiState.value
            assertFalse(state.hasChangesNumDaysAfterRedactingTradeData)
        }

    @Test
    fun `when num days after redacting cancel then restores original value`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("120"))
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.hasChangesNumDaysAfterRedactingTradeData)

            // When
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataCancel)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("90", state.numDaysAfterRedactingTradeData.value) // Original value restored
            assertFalse(state.hasChangesNumDaysAfterRedactingTradeData)
        }

    // ========== PoW Factor Tests ==========

    @Test
    fun `when pow factor changes to valid value then updates state and tracks changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - change from 1 to 5
            presenter.onAction(SettingsUiAction.OnPowFactorChange("5"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("5", state.powFactor.value)
            assertTrue(state.hasChangesPowFactor)
        }

    @Test
    fun `when pow factor is below minimum then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - negative value
            presenter.onAction(SettingsUiAction.OnPowFactorChange("-1"))
            presenter.onAction(SettingsUiAction.OnPowFactorFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.powFactor.errorMessage)
        }

    @Test
    fun `when pow factor is above maximum then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - above 160000
            presenter.onAction(SettingsUiAction.OnPowFactorChange("200000"))
            presenter.onAction(SettingsUiAction.OnPowFactorFocus(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.powFactor.errorMessage)
        }

    @Test
    fun `when pow factor save succeeds then updates original value and clears changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setDifficultyAdjustmentFactor(5.0) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnPowFactorChange("5"))
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnPowFactorSave)
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setDifficultyAdjustmentFactor(5.0) }
            val state = presenter.uiState.value
            assertFalse(state.hasChangesPowFactor)
        }

    @Test
    fun `when pow factor cancel then restores original value`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnPowFactorChange("5"))
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.hasChangesPowFactor)

            // When
            presenter.onAction(SettingsUiAction.OnPowFactorCancel)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("1.0", state.powFactor.value) // Original value restored (Double.toString())
            assertFalse(state.hasChangesPowFactor)
        }

    // ========== Animation Setting Tests ==========

    @Test
    fun `when use animations toggled then updates state and calls service`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setUseAnimations(false) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnUseAnimationsChange(false))
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setUseAnimations(false) }
            val state = presenter.uiState.value
            assertFalse(state.useAnimations)
        }

    @Test
    fun `when setting use animations fails then reverts state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setUseAnimations(false) } returns Result.failure(Exception("Error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnUseAnimationsChange(false))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.useAnimations) // Reverted to original
        }

    // ========== PoW Ignore Tests ==========

    @Test
    fun `when ignore pow toggled then updates state and calls service`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setIgnoreDiffAdjustmentFromSecManager(true) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnIgnorePowChange(true))
            advanceUntilIdle()

            // Then
            coVerify { settingsServiceFacade.setIgnoreDiffAdjustmentFromSecManager(true) }
            val state = presenter.uiState.value
            assertTrue(state.ignorePow)
        }

    @Test
    fun `when setting ignore pow fails then reverts state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)
            coEvery { settingsServiceFacade.setIgnoreDiffAdjustmentFromSecManager(true) } returns Result.failure(Exception("Error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SettingsUiAction.OnIgnorePowChange(true))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.ignorePow) // Reverted to original
        }

    // ========== Complex Scenario Tests ==========

    @Test
    fun `when multiple fields have changes then tracks each independently`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When - change multiple fields
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("7"))
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("120"))
            presenter.onAction(SettingsUiAction.OnPowFactorChange("5"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.hasChangesTradePriceTolerance)
            assertTrue(state.hasChangesNumDaysAfterRedactingTradeData)
            assertTrue(state.hasChangesPowFactor)
        }

    @Test
    fun `when cancel one field then others still have changes`() =
        runTest(testDispatcher) {
            // Given
            coEvery { settingsServiceFacade.getSettings() } returns Result.success(sampleSettings)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(SettingsUiAction.OnTradePriceToleranceChange("7"))
            presenter.onAction(SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange("120"))
            advanceUntilIdle()

            // When - cancel only trade price tolerance
            presenter.onAction(SettingsUiAction.OnTradePriceToleranceCancel)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.hasChangesTradePriceTolerance)
            assertTrue(state.hasChangesNumDaysAfterRedactingTradeData)
        }
}
